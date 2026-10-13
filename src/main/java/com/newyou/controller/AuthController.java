package com.newyou.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
            //    SMS_PROVIDER=solapi 로 실행하면 이 값은 응답에 포함되지 않습니다.
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

        // 아이디 확인 (규칙 + 중복)
        String loginId;
        try {
            loginId = userService.normalizeLoginId(registerRequest.getLoginId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
        if (userService.isLoginIdTaken(loginId)) {
            return ResponseEntity.badRequest().body(Map.of("message", "이미 사용 중인 아이디예요."));
        }

        User newUser = new User();
        newUser.setPhoneNumber(phone);
        newUser.setLoginId(loginId);
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
        boolean byId = loginRequest.getLoginId() != null && !loginRequest.getLoginId().isBlank();
        log.info("로그인 시도: {} = {}", byId ? "아이디" : "전화번호",
                byId ? loginRequest.getLoginId() : loginRequest.getPhoneNumber());

        // 아이디로 로그인하거나, 예전처럼 전화번호로 로그인해요
        User authenticatedUser = byId
                ? userService.authenticateByLoginId(loginRequest.getLoginId(), loginRequest.getPassword())
                : userService.authenticate(
                loginRequest.getPhoneNumber() == null ? null : loginRequest.getPhoneNumber().replaceAll("[^0-9]", ""),
                loginRequest.getPassword());

        if (authenticatedUser != null) {
            log.info("✅ 인증 성공: 사용자 ID = {}, 이름 = {}",
                    authenticatedUser.getId(), authenticatedUser.getName());

            // 🚨 수정: createToken 메서드 사용 (전화번호를 username으로 사용)
            String jwt = jwtTokenProvider.createToken(authenticatedUser.getPhoneNumber());

            log.info("✅ JWT 토큰 생성 완료");
            log.info("토큰 앞부분: {}...", jwt.substring(0, Math.min(30, jwt.length())));

            // 사용자 정보 반환 (비밀번호 제외)
            Map<String, Object> response = new HashMap<>();
            response.put("token", jwt);
            response.put("id", authenticatedUser.getId());
            response.put("phoneNumber", authenticatedUser.getPhoneNumber());
            response.put("loginId", authenticatedUser.getLoginId() != null ? authenticatedUser.getLoginId() : "");
            response.put("name", authenticatedUser.getName());
            response.put("profileImage", authenticatedUser.getProfileImage() != null
                    ? authenticatedUser.getProfileImage()
                    : "");

            log.info("========================================");
            return ResponseEntity.ok(response);

        } else {
            log.warn("❌ 인증 실패: 전화번호 또는 비밀번호 불일치");
            log.info("========================================");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body("아이디(전화번호) 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    // =======================================================================
    // 아이디 중복 확인 · 아이디 찾기 · 비밀번호 다시 만들기
    // =======================================================================

    /** GET /api/auth/check-id?loginId=khunsan → { available: true/false, message } */
    @GetMapping("/check-id")
    public ResponseEntity<?> checkLoginId(@RequestParam String loginId) {
        try {
            String id = userService.normalizeLoginId(loginId);
            boolean taken = userService.isLoginIdTaken(id);
            return ResponseEntity.ok(Map.of(
                    "available", !taken,
                    "message", taken ? "이미 사용 중인 아이디예요." : "사용할 수 있는 아이디예요."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(Map.of("available", false, "message", e.getMessage()));
        }
    }

    /**
     * POST /api/auth/find-id { phoneNumber }
     * 먼저 /send-code, /verify-code 로 휴대폰 인증을 마친 다음 불러요.
     */
    @PostMapping("/find-id")
    public ResponseEntity<?> findLoginId(@RequestBody PhoneRequest request) {
        try {
            Optional<User> user = userService.findByVerifiedPhone(request.getPhoneNumber());
            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("message", "이 번호로 가입한 계정이 없어요."));
            }
            User u = user.get();
            Map<String, Object> body = new HashMap<>();
            body.put("loginId", u.getLoginId() != null ? u.getLoginId() : "");
            body.put("name", u.getName());
            body.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
            return ResponseEntity.ok(body);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * POST /api/auth/reset-password { loginId, phoneNumber, newPassword }
     * 먼저 /send-code, /verify-code 로 휴대폰 인증을 마친 다음 불러요.
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        try {
            userService.resetPassword(body.get("loginId"), body.get("phoneNumber"), body.get("newPassword"));
            return ResponseEntity.ok(Map.of("message", "비밀번호가 바뀌었어요. 새 비밀번호로 로그인해 주세요."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}