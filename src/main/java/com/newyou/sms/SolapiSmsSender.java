package com.newyou.sms;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 솔라피(SOLAPI)를 이용한 실제 문자 발송기.
 * sms.provider=solapi 일 때 사용됩니다.
 *
 * 필요한 설정 (환경변수 권장):
 * SOLAPI_API_KEY, SOLAPI_API_SECRET : 솔라피 콘솔 > 개발/연동 > API Key 관리
 * SOLAPI_SENDER : 솔라피에 사전 등록한 발신번호 (숫자만)
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sms.provider", havingValue = "solapi")
public class SolapiSmsSender implements SmsSender {

    private static final String SEND_URL = "https://api.solapi.com/messages/v4/send-many/detail";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");
    private static final String SALT_CHARS = "1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private final String apiKey;
    private final String apiSecret;
    private final String sender;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final SecureRandom random = new SecureRandom();

    public SolapiSmsSender(
            @Value("${sms.solapi.api-key:}") String apiKey,
            @Value("${sms.solapi.api-secret:}") String apiSecret,
            @Value("${sms.solapi.sender:}") String sender,
            ObjectMapper objectMapper) {
        if (apiKey.isBlank() || apiSecret.isBlank() || sender.isBlank()) {
            throw new IllegalStateException(
                    "sms.provider=solapi 인데 SOLAPI_API_KEY / SOLAPI_API_SECRET / SOLAPI_SENDER 중 비어 있는 값이 있습니다.");
        }
        this.apiKey = apiKey.trim();
        this.apiSecret = apiSecret.trim();
        this.sender = sender.replaceAll("[^0-9]", "");
        this.objectMapper = objectMapper;
        log.info("✅ SMS 발송기: SOLAPI 모드 (발신번호 {})", maskPhone(this.sender));
    }

    @Override
    public void send(String to, String text) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "messages", List.of(Map.of(
                            "to", to,
                            "from", sender,
                            "text", text))));

            HttpRequest request = HttpRequest.newBuilder(URI.create(SEND_URL))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", buildAuthorizationHeader())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() / 100 != 2) {
                log.error("❌ SOLAPI 응답 오류 {}: {}", response.statusCode(), response.body());
                throw new SmsSendException("문자 발송 서비스 오류 (HTTP " + response.statusCode() + ")");
            }

            // 요청은 접수됐지만 개별 메시지가 실패한 경우 (예: 미등록 발신번호, 잔액 부족)
            JsonNode failed = objectMapper.readTree(response.body()).path("failedMessageList");
            if (failed.isArray() && failed.size() > 0) {
                log.error("❌ SOLAPI 메시지 접수 실패: {}", failed);
                throw new SmsSendException("문자 발송에 실패했습니다: " + failed.get(0).path("statusMessage").asText());
            }

            log.info("✅ 인증 문자 발송 완료 → {}", maskPhone(to));

        } catch (SmsSendException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SmsSendException("문자 발송이 중단되었습니다.", e);
        } catch (Exception e) {
            log.error("❌ SOLAPI 호출 실패", e);
            throw new SmsSendException("문자 발송 서비스에 연결하지 못했습니다.", e);
        }
    }

    /**
     * SOLAPI HMAC-SHA256 인증 헤더
     * signature = HEX( HMAC_SHA256(key = apiSecret, data = date + salt) )
     */
    private String buildAuthorizationHeader() throws Exception {
        // 공식 SDK와 같은 형식: 2026-10-06T17:30:00+09:00 (UTC여도 'Z' 대신 +00:00)
        String date = OffsetDateTime.now().truncatedTo(ChronoUnit.SECONDS)
                .format(DATE_FORMAT);
        StringBuilder salt = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            salt.append(SALT_CHARS.charAt(random.nextInt(SALT_CHARS.length())));
        }

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(
                mac.doFinal((date + salt).getBytes(StandardCharsets.UTF_8)));

        return "HMAC-SHA256 apiKey=" + apiKey + ", date=" + date + ", salt=" + salt + ", signature=" + signature;
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7)
            return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}