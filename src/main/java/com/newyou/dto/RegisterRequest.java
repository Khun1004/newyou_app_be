package com.newyou.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {
    private String phoneNumber;
    private String loginId; // 로그인 아이디
    private String name;
    private String password;
}