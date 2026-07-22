package com.newyou.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

// 휴대폰 인증 및 회원가입 요청 시 사용되는 통합 DTO
@Data
@NoArgsConstructor
public class AuthRequest {

    // 1. 휴대폰 인증 요청 및 확인 시 사용
    private String phoneNumber;

    // 2. 인증번호 확인 시 사용
    private String code;

    // 3. 회원가입 시 사용
    private String name;
    private String password;
}