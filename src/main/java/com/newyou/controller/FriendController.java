package com.newyou.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.newyou.dto.FriendRequest;
import com.newyou.entity.Friend;
import com.newyou.entity.User;
import com.newyou.service.FriendService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    @Value("${file.upload-dir.friend-profile:./uploads/friendprofiles}")
    private String friendProfileUploadDir;

    @Autowired
    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    // =======================================================================
    // 0. 친구 프로필 이미지 업로드 (POST /api/friends/upload-image)
    // =======================================================================
    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadFriendProfileImage(
            @AuthenticationPrincipal User user,
            @RequestParam("image") MultipartFile imageFile) {

        log.info("========================================");
        log.info("📸 친구 프로필 이미지 업로드 요청");
        log.info("- User ID: {}", user.getId());
        log.info("- User Phone: {}", user.getPhoneNumber());
        log.info("- File Name: {}", imageFile.getOriginalFilename());
        log.info("- File Size: {} bytes", imageFile.getSize());
        log.info("- Content Type: {}", imageFile.getContentType());
        log.info("========================================");

        try {
            // 파일이 비어있는지 확인
            if (imageFile.isEmpty()) {
                log.error("❌ 업로드된 파일이 비어있습니다.");
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "업로드된 파일이 비어있습니다."));
            }

            // 1. 업로드 디렉토리 생성
            Path uploadPath = Paths.get(friendProfileUploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("✅ 디렉토리 생성: {}", uploadPath.toAbsolutePath());
            }

            // 2. 파일 확장자 검증
            String originalFilename = imageFile.getOriginalFilename();
            if (originalFilename == null || originalFilename.isEmpty()) {
                log.error("❌ 파일명이 유효하지 않습니다.");
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "파일명이 유효하지 않습니다."));
            }

            String extension = "";
            int lastDotIndex = originalFilename.lastIndexOf('.');
            if (lastDotIndex > 0) {
                extension = originalFilename.substring(lastDotIndex).toLowerCase();
            }

            // 허용된 확장자 검증
            if (!extension.matches("\\.(jpg|jpeg|png|gif)$")) {
                log.error("❌ 허용되지 않은 파일 형식: {}", extension);
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "허용되지 않은 파일 형식입니다. (jpg, jpeg, png, gif만 가능)"));
            }

            // 3. 고유 파일명 생성
            String uniqueFileName = "friend_" + System.currentTimeMillis() + "_"
                    + UUID.randomUUID().toString().substring(0, 8) + extension;
            Path filePath = uploadPath.resolve(uniqueFileName);

            log.info("💾 파일 저장 경로: {}", filePath.toAbsolutePath());

            // 4. 파일 저장
            Files.copy(imageFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("✅ 파일 저장 완료: {}", filePath.toAbsolutePath());

            // 5. 접근 가능한 URL 생성
            String fileUrl = "/uploads/friendprofiles/" + uniqueFileName;

            log.info("========================================");
            log.info("✅ 친구 프로필 이미지 업로드 성공");
            log.info("- 저장된 파일: {}", uniqueFileName);
            log.info("- 접근 URL: {}", fileUrl);
            log.info("========================================");

            return ResponseEntity.ok(Map.of(
                    "message", "이미지 업로드 성공",
                    "imageUrl", fileUrl));

        } catch (IOException e) {
            log.error("========================================");
            log.error("❌ 이미지 업로드 실패");
            log.error("- 오류 메시지: {}", e.getMessage());
            log.error("- 스택 트레이스:");
            e.printStackTrace();
            log.error("========================================");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "이미지 업로드 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    // =======================================================================
    // 1. 친구 추가 (POST /api/friends)
    // =======================================================================
    @PostMapping
    public ResponseEntity<?> addFriend(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody FriendRequest request) {

        log.info("친구 추가 요청: User ID = {}, Nickname = {}", user.getId(), request.getNickname());

        try {
            Friend newFriend = friendService.addFriend(user.getId(), request);
            log.info("✅ 친구 추가 성공: Friend ID = {}", newFriend.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(newFriend);

        } catch (IllegalArgumentException e) {
            log.warn("❌ 친구 추가 실패 (400 Bad Request): {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ 친구 추가 중 서버 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "친구 추가 중 서버 오류가 발생했습니다."));
        }
    }

    // =======================================================================
    // 2. 친구 목록 조회 (GET /api/friends)
    // =======================================================================
    @GetMapping
    public ResponseEntity<List<Friend>> getFriends(@AuthenticationPrincipal User user) {
        log.info("친구 목록 조회 요청: User ID = {}", user.getId());
        List<Friend> friends = friendService.getFriends(user.getId());
        log.info("✅ 친구 목록 조회 성공: {}명", friends.size());
        return ResponseEntity.ok(friends);
    }

    // =======================================================================
    // 3. 친구 상세 조회 (GET /api/friends/{friendId})
    // =======================================================================
    @GetMapping("/{friendId}")
    public ResponseEntity<?> getFriend(
            @AuthenticationPrincipal User user,
            @PathVariable Long friendId) {

        log.info("친구 상세 조회 요청: User ID = {}, Friend ID = {}", user.getId(), friendId);

        try {
            Friend friend = friendService.getFriend(user.getId(), friendId);
            log.info("✅ 친구 상세 조회 성공: Friend ID = {}", friend.getId());
            return ResponseEntity.ok(friend);
        } catch (IllegalArgumentException e) {
            log.warn("❌ 친구 상세 조회 실패 (404 Not Found/403 Forbidden): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    // =======================================================================
    // 4. 친구 정보 수정 (PUT /api/friends/{friendId})
    // =======================================================================
    @PutMapping("/{friendId}")
    public ResponseEntity<?> updateFriend(
            @AuthenticationPrincipal User user,
            @PathVariable Long friendId,
            @Valid @RequestBody FriendRequest request) {

        log.info("친구 수정 요청: User ID = {}, Friend ID = {}", user.getId(), friendId);

        try {
            Friend updatedFriend = friendService.updateFriend(user.getId(), friendId, request);
            log.info("✅ 친구 수정 성공: Friend ID = {}", updatedFriend.getId());
            return ResponseEntity.ok(updatedFriend);

        } catch (IllegalArgumentException e) {
            log.warn("❌ 친구 수정 실패 (400 Bad Request/404 Not Found): {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ 친구 수정 중 서버 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "친구 수정 중 서버 오류가 발생했습니다."));
        }
    }

    // =======================================================================
    // 5. 친구 삭제 (DELETE /api/friends/{friendId})
    // =======================================================================
    @DeleteMapping("/{friendId}")
    public ResponseEntity<?> deleteFriend(
            @AuthenticationPrincipal User user,
            @PathVariable Long friendId) {

        log.info("친구 삭제 요청: User ID = {}, Friend ID = {}", user.getId(), friendId);

        try {
            friendService.deleteFriend(user.getId(), friendId);
            log.info("✅ 친구 삭제 성공: Friend ID = {}", friendId);
            return ResponseEntity.ok(Map.of("message", "친구가 성공적으로 삭제되었습니다."));

        } catch (IllegalArgumentException e) {
            log.warn("❌ 친구 삭제 실패 (404 Not Found/403 Forbidden): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        }
    }

    // =======================================================================
    // 6. 오늘 생일인 친구 조회 (GET /api/friends/today-birthdays)
    // =======================================================================
    @GetMapping("/today-birthdays")
    public ResponseEntity<List<Friend>> getTodayBirthdays(@AuthenticationPrincipal User user) {
        log.info("오늘 생일인 친구 목록 조회 요청: User ID = {}", user.getId());
        List<Friend> friends = friendService.getTodayBirthdays(user.getId());
        log.info("✅ 오늘 생일인 친구 목록 조회 성공: {}명", friends.size());
        return ResponseEntity.ok(friends);
    }
}