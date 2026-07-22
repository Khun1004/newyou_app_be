package com.newyou.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlanRequest {
    private String title;
    private String content;
    private String color;
    private String planDate; // ⭐⭐ 추가해야 합니다.
}