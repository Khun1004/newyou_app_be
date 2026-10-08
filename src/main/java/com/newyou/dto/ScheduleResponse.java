package com.newyou.dto;

import com.newyou.entity.TimetableSchedule;

import lombok.Getter;

/** 앱으로 돌려주는 시간표 일정 정보 */
@Getter
public class ScheduleResponse {
    private final String id;
    private final String title;
    private final String day;
    private final String time;
    private final Double duration;
    private final String color;

    public ScheduleResponse(TimetableSchedule s) {
        this.id = String.valueOf(s.getId());
        this.title = s.getTitle();
        this.day = s.getDay();
        this.time = s.getTime();
        this.duration = s.getDuration();
        this.color = s.getColor();
    }
}