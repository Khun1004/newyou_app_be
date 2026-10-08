package com.newyou.sms;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * 개발용 문자 발송기: 실제로 문자를 보내지 않고 서버 콘솔에 내용을 출력합니다.
 * sms.provider=console 이거나 값이 없을 때 사용됩니다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sms.provider", havingValue = "console", matchIfMissing = true)
public class ConsoleSmsSender implements SmsSender {

    public ConsoleSmsSender() {
        log.warn("⚠️ SMS 발송기: CONSOLE 모드 (실제 문자는 발송되지 않고 서버 콘솔에만 출력됩니다)");
    }

    @Override
    public void send(String to, String text) {
        log.info("[SMS 콘솔 모드] 받는 번호: {} / 내용: {}", to, text);
    }

    @Override
    public boolean deliversRealSms() {
        return false; // 콘솔 모드: 실제 문자가 가지 않으므로 앱 화면에 인증번호를 표시
    }
}