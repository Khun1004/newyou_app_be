package com.newyou.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {
    private String phoneNumber; // 전화번호로 로그인할 때
    private String loginId;     // 아이디로 로그인할 때 (둘 중 하나만 보내도 돼요)
    private String password;
}