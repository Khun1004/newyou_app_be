package com.newyou.dto;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newyou.entity.Friend;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 친구 엔티티 정보를 클라이언트에게 응답하기 위한 DTO입니다.
 * 클라이언트의 Friend 인터페이스에 맞게 필드를 정의합니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendResponse {

    // 클라이언트가 String 타입의 ID를 기대하므로 String으로 정의
    private String id;
    private String nickname;
    private Integer birthMonth; // 클라이언트가 별도의 month/day 필드를 기대
    private Integer birthDay;
    private List<String> profileColor; // 클라이언트가 List<String>을 기대
    private String profileImage;
    private String memo;

    // 💡 에러 응답을 위해 추가된 필드: 오류 메시지를 담습니다.
    private String errorMessage;

    /**
     * Friend 엔티티를 FriendResponse DTO로 변환합니다.
     */
    public static FriendResponse fromEntity(Friend friend) {
        // profileColor 문자열을 List<String>으로 역직렬화
        List<String> colorList = null;
        if (friend.getProfileColor() != null) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                // DB에 저장된 JSON 문자열을 List<String>으로 파싱
                colorList = Arrays
                        .asList(Optional.ofNullable(mapper.readValue(friend.getProfileColor(), String[].class))
                                .orElse(new String[0]));
            } catch (JsonProcessingException e) {
                // JSON 파싱 오류 시 빈 리스트 사용
                colorList = List.of();
            }
        } else {
            colorList = List.of();
        }

        return FriendResponse.builder()
                .id(String.valueOf(friend.getId())) // Long -> String 변환
                .nickname(friend.getNickname())
                .birthMonth(friend.getBirthMonth())
                .birthDay(friend.getBirthDay())
                .profileColor(colorList)
                .profileImage(friend.getProfileImage())
                .memo(friend.getMemo())
                // fromEntity는 성공 응답이므로 errorMessage는 null
                .errorMessage(null)
                .build();
    }

    /**
     * 💡 [추가] 컨트롤러에서 발생하는 오류를 처리하기 위한 정적 메서드입니다.
     * FriendController에서 FriendResponse.error(e.getMessage()) 형태로 사용됩니다.
     */
    public static FriendResponse error(String message) {
        return FriendResponse.builder()
                // 성공 응답 필드는 비워두고, 에러 메시지만 채웁니다.
                .errorMessage(message)
                .build();
    }
}