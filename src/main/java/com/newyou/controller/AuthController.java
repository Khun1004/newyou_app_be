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
            userService.sendVerificationCode(phoneNumber);
            return ResponseEntity.ok("인증번호가 성공적으로 발송되었습니다.");
        } catch (Exception e) {
            log.error("인증번호 발송 중 오류 발생: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("인증번호 발송 중 오류가 발생했습니다.");
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
        if (userService.isPhoneNumberExists(registerRequest.getPhoneNumber())) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("이미 등록된 전화번호입니다.");
        }

        User newUser = new User();
        newUser.setPhoneNumber(registerRequest.getPhoneNumber());
        newUser.setName(registerRequest.getName());
        newUser.setPassword(registerRequest.getPassword());

        try {
            userService.registerUser(newUser);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body("회원가입이 성공적으로 완료되었습니다.");
        } catch (Exception e) {
            log.error("사용자 등록 중 오류 발생: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("사용자 등록 중 오류가 발생했습니다.");
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