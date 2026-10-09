package com.newyou.dto;

import com.newyou.entity.MoneyRecord;

import lombok.Getter;

/** 앱으로 돌려주는 가계부 기록 */
@Getter
public class MoneyResponse {
    private final String id;
    private final String type;
    private final Long amount;
    private final String category;
    private final String memo;
    private final String date;

    public MoneyResponse(MoneyRecord r) {
        this.id = String.valueOf(r.getId());
        this.type = r.getType();
        this.amount = r.getAmount();
        this.category = r.getCategory();
        this.memo = r.getMemo();
        this.date = r.getDate();
    }
}