package com.newyou.dto;

import com.newyou.entity.Plan;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlanResponse {
    private Long id;
    private String title;
    private String content;
    private String color;
    private String planDate; // ⭐⭐ planDate 필드 추가 ⭐⭐
    private String date; // React의 Plan 인터페이스와 맞추기 위해 String으로 변경 (실제로는 createdAt)

    public PlanResponse(Plan plan) {
        this.id = plan.getId();
        this.title = plan.getTitle();
        this.content = plan.getContent();
        this.color = plan.getColor();
        this.planDate = plan.getPlanDate(); // ⭐⭐ planDate 매핑 추가 ⭐⭐
        // React의 date 필드가 LocalDateTime이 아닌 String이므로 문자열로 변환
        this.date = plan.getCreatedAt() != null ? plan.getCreatedAt().toString() : null;
    }
}