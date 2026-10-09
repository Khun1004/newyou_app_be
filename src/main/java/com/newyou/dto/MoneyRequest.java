package com.newyou.dto;

import lombok.Getter;
import lombok.Setter;

/** 앱에서 보내는 가계부 기록 */
@Getter
@Setter
public class MoneyRequest {
    private String type; // INCOME / EXPENSE
    private Long amount;
    private String category;
    private String memo;
    private String date; // YYYY-MM-DD
}