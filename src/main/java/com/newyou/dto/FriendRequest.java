package com.newyou.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FriendRequest {

    // 닉네임 (필수)
    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 1, max = 50, message = "닉네임은 1자 이상 50자 이하여야 합니다.")
    private String nickname;

    // 💡 수정: LocalDate 대신 String으로 직접 받습니다. (클라이언트 'MM-dd' 형식 수용)
    // @JsonFormat(pattern = "MM-dd") // 💡 제거
    private String birthdate;

    // 프로필 색상 필드 (필수)
    @NotBlank(message = "프로필 색상 정보는 필수입니다.")
    private String profileColor;

    // 프로필 이미지 URL (선택) - FileService에서 반환된 URL이 컨트롤러에서 설정됩니다.
    private String profileImage;

    // 메모 (선택)
    @Size(max = 1000, message = "메모는 1000자 이하여야 합니다.")
    private String memo;

    // NOTE: 기존 profileData 필드가 사용되지 않는다면 제거해도 됩니다. (현재 코드에서는 제거됨)
}