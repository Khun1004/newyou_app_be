package com.newyou.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.newyou.dto.MyPageResponse;
import com.newyou.entity.User;
import com.newyou.service.MyPageService; // MyPage 로직을 처리할 서비스 가정

/**
 * 마이페이지 데이터를 제공하는 API 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/mypage")
public class MyPageController {

    private final MyPageService myPageService;

    @Autowired
    public MyPageController(MyPageService myPageService) {
        this.myPageService = myPageService;
    }

    /**
     * 마이페이지에 필요한 사용자 정보 및 통계 데이터를 반환합니다.
     * * @param user 현재 인증된 사용자 정보 (Spring Security를 사용한다고 가정)
     * * @return MyPageResponse DTO
     */
    @GetMapping
    public ResponseEntity<MyPageResponse> getMyPageData(@AuthenticationPrincipal User user) {

        // 💡 실제로는 Spring Security 설정을 통해 JWT 등에서 User 객체를 받아와야 합니다.
        // 현재는 편의상 UserService의 내용을 사용하여 임시 User 객체를 생성하거나,
        // @AuthenticationPrincipal로 넘어온 User를 사용합니다.

        // ⚠️ 주의: 현재 User 엔티티는 Long id, String phoneNumber, String password, String name
        // 필드만 있습니다.
        // createdAt, profileImage 필드는 엔티티에 존재하지 않습니다.

        // User 객체의 필드가 모두 설정되어있다고 가정하고 MyPageService를 호출합니다.
        // Spring Security 컨텍스트에서 User 객체를 제대로 가져오지 못할 경우를 대비하여 목업 데이터를 사용합니다.

        User authenticatedUser = user;
        if (authenticatedUser == null) {
            // 인증이 안 된 경우 (테스트를 위해 목업 사용자 생성)
            authenticatedUser = new User();
            authenticatedUser.setId(1L);
            authenticatedUser.setPhoneNumber("01012345678");
            authenticatedUser.setName("테스트닉네임");
            authenticatedUser.setProfileImage("/profileImages/default.png"); // ✅ 목업 데이터에 이미지 경로 추가
            // password는 응답에 포함되지 않으므로 설정 불필요
            // createdAt 필드는 User 엔티티에 없으므로 DTO 생성 시 null 처리됩니다.
        }

        MyPageResponse response = myPageService.getMyPageData(authenticatedUser);

        return ResponseEntity.ok(response);
    }
}