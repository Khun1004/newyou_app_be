package com.newyou.dto;

import java.util.Date;
import java.util.List;

import com.newyou.entity.User;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyPageResponse {

        // --- 사용자 정보 ---
        private String nickname;
        private String phoneNumber;
        private Date createdAt;
        private String profileImage; // ✅ 프로필 이미지 경로 필드

        // --- 통계 정보 ---
        private List<Stat> statistics; // 통계 목록

        @Getter
        @Builder
        public static class Stat {
                private String id;
                private String label;
                private String value;
                private String icon;
                private String color;
        }

        /**
         * User 엔티티와 기타 정보를 기반으로 MyPageResponse DTO를 생성합니다.
         * 실제 서비스에서는 알람 갯수, 완료된 계획 갯수 등은 별도의 서비스에서 조회해야 합니다.
         */
        public static MyPageResponse fromUserAndStats(User user, int completedPlans, int alarmCount, int anniversaries,
                        int consecutiveDays) {

                // 닉네임 필드가 User 엔티티에 없으므로 name 필드를 사용합니다.
                // 실제 엔티티에 nickname 필드가 있다면 그걸 사용해야 합니다.
                String nickname = user.getName();
                String profileImage = user.getProfileImage(); // ✅ User 엔티티에서 profileImage 가져오기

                // MyScreen.tsx의 statistics 구조를 따릅니다.
                List<Stat> stats = List.of(
                                Stat.builder().id("1").label("완료한 계획").value(String.valueOf(completedPlans))
                                                .icon("checkmark-circle")
                                                .color("#4ECDC4").build(),
                                Stat.builder().id("2").label("설정한 알람").value(String.valueOf(alarmCount)).icon("alarm")
                                                .color("#FF6B6B")
                                                .build(),
                                Stat.builder().id("3").label("기념일 등록").value(String.valueOf(anniversaries)).icon("gift")
                                                .color("#9B59B6").build(),
                                Stat.builder().id("4").label("연속 사용일").value(consecutiveDays + "일").icon("flame")
                                                .color("#FFA726")
                                                .build());

                return MyPageResponse.builder()
                                .nickname(nickname)
                                .phoneNumber(user.getPhoneNumber())
                                // User 엔티티에 createdAt 필드가 없으므로 현재는 null로 가정합니다.
                                // 실제라면 엔티티에 생성일시 필드(e.g., @CreationTimestamp)가 있어야 합니다.
                                .createdAt(null)
                                // ✅ User 엔티티에서 가져온 profileImage를 DTO에 설정합니다.
                                .profileImage(profileImage)
                                .statistics(stats)
                                .build();
        }
}