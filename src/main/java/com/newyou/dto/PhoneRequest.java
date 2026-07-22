package com.newyou.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

// 휴대폰 인증번호 요청 시 사용되는 DTO
@Data
@NoArgsConstructor
public class PhoneRequest {
    // 클라이언트에서 전달받는 전화번호 필드
    private String phoneNumber;
}