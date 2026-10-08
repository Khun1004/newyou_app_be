package com.newyou.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.newyou.dto.LoginRequest;
import com.newyou.dto.PhoneRequest;
import com.newyou.dto.RegisterRequest;
import com.newyou.dto.VerifyRequest;
import com.newyou.entity.User;
import com.newyou.security.JwtTokenProvider;
import com.newyou.service.UserService;
import com.newyou.sms.SmsSendException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public AuthController(UserService userService, JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/send-code")
    public ResponseEntity<?> sendCode(@RequestBody PhoneRequest phoneRequest) {
        String phoneNumber = phoneRequest.getPhoneNumber();

        try {
            String code = userService.sendVerificationCode(phoneNumber);

            if (userService.isRealSmsMode()) {
                return ResponseEntity.ok(Map.of("message", "인증번호가 발송되었습니다. 문자를 확인해 주세요."));
            }

            // 🧪 콘솔(개발) 모드: 실제 문자가 가지 않으므로 앱 화면에 인증번호를 보여줍니다.
            // SMS_PROVIDER=solapi 로 실행하면 이 값은 응답에 포함되지 않습니다.
            return ResponseEntity.ok(Map.of(
                    "message", "[개발 모드] 인증번호: " + code,
                    "devCode", code));
        } catch (IllegalArgumentException e) {
            // 번호 형식 오류, 재발송 대기 중
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (SmsSendException e) {
            log.error("인증 문자 발송 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "문자 발송에 실패했습니다. 잠시 후 다시 시도해 주세요."));
        } catch (Exception e) {
            log.error("인증번호 발송 중 오류 발생", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "인증번호 발송 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestBody VerifyRequest verifyRequest) {
        String phoneNumber = verifyRequest.getPhoneNumber();
        String code = verifyRequest.getCode();

        try {
            if (userService.verifyCode(phoneNumber, code)) {
                return ResponseEntity.ok("휴대폰 인증이 성공적으로 완료되었습니다.");
            } else {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body("인증번호가 일치하지 않습니다.");
            }
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(e.getMessage());
        } catch (Exception e) {
            log.error("인증 코드 확인 중 오류 발생: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("인증 확인 중 오류가 발생했습니다.");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest registerRequest) {
        String phone = registerRequest.getPhoneNumber() == null
                ? ""
                : registerRequest.getPhoneNumber().replaceAll("[^0-9]", "");

        try {
            // 서버에서도 휴대폰 인증 완료 여부를 확인합니다. (앱을 거치지 않은 가입 요청 차단)
            if (!userService.isPhoneVerified(phone)) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "휴대폰 인증을 먼저 완료해 주세요."));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }

        if (userService.isPhoneNumberExists(phone)) {
            return ResponseEntity.badRequest().body(Map.of("message", "이미 등록된 전화번호입니다."));
        }

        User newUser = new User();
        newUser.setPhoneNumber(phone);
        newUser.setName(registerRequest.getName());
        newUser.setPassword(registerRequest.getPassword());

        try {
            userService.registerUser(newUser);
            userService.consumePhoneVerification(phone);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(Map.of("message", "회원가입이 성공적으로 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            // 닉네임 중복 등
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("사용자 등록 중 오류 발생", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "사용자 등록 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        log.info("========================================");
        log.info("로그인 시도: 전화번호 = {}", loginRequest.getPhoneNumber());

        User authenticatedUser = userService.authenticate(
                loginRequest.getPhoneNumber(),
                loginRequest.getPassword());

        if (authenticatedUser != null) {
            log.info("✅ 인증 성공: 사용자 ID = {}, 이름 = {}",
                    authenticatedUser.getId(), authenticatedUser.getName());

            // 🚨 수정: createToken 메서드 사용 (전화번호를 username으로 사용)
            String jwt = jwtTokenProvider.createToken(authenticatedUser.getPhoneNumber());

            log.info("✅ JWT 토큰 생성 완료");
            log.info("토큰 앞부분: {}...", jwt.substring(0, Math.min(30, jwt.length())));

            // 사용자 정보 반환 (비밀번호 제외)
            Map<String, Object> response = Map.of(
                    "token", jwt,
                    "id", authenticatedUser.getId(),
                    "phoneNumber", authenticatedUser.getPhoneNumber(),
                    "name", authenticatedUser.getName(),
                    "profileImage", authenticatedUser.getProfileImage() != null
                            ? authenticatedUser.getProfileImage()
                            : "");

            log.info("========================================");
            return ResponseEntity.ok(response);

        } else {
            log.warn("❌ 인증 실패: 전화번호 또는 비밀번호 불일치");
            log.info("========================================");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("전화번호 또는 비밀번호가 올바르지 않습니다.");
        }
    }
}