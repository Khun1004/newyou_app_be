package com.newyou.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.newyou.dto.ProfileUpdateRequest;
import com.newyou.entity.User;
import com.newyou.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal User user,
            @RequestBody ProfileUpdateRequest request) {

        // 요청 수신 로그
        System.out.println("========================================");
        System.out.println("프로필 업데이트 API 호출");
        System.out.println("========================================");
        System.out.println("인증 상태: " + (user != null ? "인증됨 (User ID: " + user.getId() + ")" : "인증 실패"));

        if (request.getNickname() != null) {
            System.out.println("요청된 닉네임: " + request.getNickname());
        }
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            System.out.println("비밀번호 변경 요청: 있음");
        }
        if (request.getProfileImage() != null) {
            System.out.println("프로필 이미지 타입: " + request.getProfileImage().getClass().getName());
            if (request.getProfileImage() instanceof String) {
                String imageStr = (String) request.getProfileImage();
                if (imageStr.startsWith("data:image")) {
                    System.out.println("프로필 이미지: Base64 데이터 (길이: " + imageStr.length() + ", 앞 50자) - "
                            + imageStr.substring(0, Math.min(50, imageStr.length())));
                } else {
                    System.out.println("프로필 이미지 경로: " + imageStr);
                }
            } else {
                System.out.println("프로필 이미지: 예상치 못한 타입 - " + request.getProfileImage());
            }
        } else {
            System.out.println("프로필 이미지: null");
        }
        System.out.println("========================================");

        // 인증 확인
        if (user == null) {
            System.err.println("ERROR: 인증되지 않은 사용자의 접근 시도");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "인증되지 않은 사용자입니다. 다시 로그인 해주세요."));
        }

        try {
            // 프로필 업데이트 실행
            User updatedUser = userService.updateProfile(user.getId(), request);

            // 응답 데이터 생성 (비밀번호 제외)
            Map<String, Object> response = new HashMap<>();
            response.put("id", updatedUser.getId());
            response.put("phoneNumber", updatedUser.getPhoneNumber());
            response.put("name", updatedUser.getName());
            response.put("profileImage", updatedUser.getProfileImage());

            if (updatedUser.getCreatedAt() != null) {
                response.put("createdAt", updatedUser.getCreatedAt().toString());
            }

            System.out.println("========================================");
            System.out.println("프로필 업데이트 성공!");
            System.out.println("업데이트된 닉네임: " + updatedUser.getName());
            System.out.println(
                    "업데이트된 이미지: " + (updatedUser.getProfileImage() != null ? updatedUser.getProfileImage() : "없음"));
            System.out.println("========================================");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // 클라이언트 오류 (잘못된 입력, 중복 닉네임 등)
            System.err.println("ERROR (400 Bad Request): " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            // 서버 오류 (이미지 저장 실패, DB 오류 등)
            System.err.println("ERROR (500 Internal Server Error): " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "프로필 업데이트 중 서버 오류가 발생했습니다."));
        }
    }
}