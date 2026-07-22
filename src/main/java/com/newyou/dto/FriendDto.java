package com.newyou.dto;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newyou.entity.Friend;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 친구 정보 생성/수정 요청에 사용되는 DTO.
 * 클라이언트의 인터페이스에 맞게 필드를 정의합니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendDto {

    // 클라이언트에서 전달받는 필드
    @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
    @Size(min = 1, max = 20, message = "닉네임은 1자 이상 20자 이하로 입력해야 합니다.")
    private String nickname;

    private Integer birthMonth; // 생일 월 (1-12)
    private Integer birthDay; // 생일 일 (1-31)

    @NotBlank(message = "프로필 색상 정보는 필수입니다.")
    private String profileColor; // 클라이언트에서 JSON stringify된 문자열로 전송됨

    private String profileImage; // 이미지 URI

    private String memo; // 친구에 대한 메모

    /**
     * FriendDto를 Friend 엔티티로 변환합니다. (새 친구 생성 시 사용)
     * 
     * @param userId 친구를 등록하는 사용자의 ID
     * @return Friend 엔티티
     */
    public Friend toEntity(Long userId) {
        return Friend.builder()
                .userId(userId)
                .nickname(this.nickname)
                .birthMonth(this.birthMonth)
                .birthDay(this.birthDay)
                .profileColor(this.profileColor)
                .profileImage(this.profileImage)
                .memo(this.memo)
                // createdAt, updatedAt, id는 Entity에서 자동 처리
                .build();
    }

    /**
     * Friend 엔티티를 클라이언트 응답용 FriendResponse DTO로 변환합니다.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FriendResponse {
        private Long id;
        private String nickname;
        private Integer birthMonth;
        private Integer birthDay;
        private List<String> profileColor; // 클라이언트에서 List<String>을 기대
        private String profileImage;
        private String memo;

        public static FriendResponse fromEntity(Friend friend) {
            // profileColor 문자열을 List<String>으로 역직렬화
            List<String> colorList = null;
            if (friend.getProfileColor() != null) {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    // DB에 "[#hex1, #hex2]" 형태로 저장된 문자열을 List<String>으로 파싱
                    colorList = Arrays.asList(mapper.readValue(friend.getProfileColor(), String[].class));
                } catch (JsonProcessingException e) {
                    // JSON 파싱 오류 시 빈 리스트 사용
                    colorList = List.of();
                }
            } else {
                colorList = List.of();
            }

            return FriendResponse.builder()
                    .id(friend.getId())
                    .nickname(friend.getNickname())
                    .birthMonth(friend.getBirthMonth())
                    .birthDay(friend.getBirthDay())
                    .profileColor(colorList)
                    .profileImage(friend.getProfileImage())
                    .memo(friend.getMemo())
                    .build();
        }
    }
}