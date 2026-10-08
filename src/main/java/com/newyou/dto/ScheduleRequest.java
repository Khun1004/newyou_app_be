package com.newyou.dto;

import lombok.Getter;
import lombok.Setter;

/** 앱에서 보내는 시간표 일정 정보 */
@Getter
@Setter
public class ScheduleRequest {
    private String title;
    private String day; // Mon ~ Sun
    private String time; // "HH:MM"
    private Double duration; // 시간 단위
    private String color;
}