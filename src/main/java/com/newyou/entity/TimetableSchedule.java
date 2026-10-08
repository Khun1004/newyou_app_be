package com.newyou.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 시간표 일정 (매주 반복되는 일정)
 * 예) 매주 월요일 09:00 부터 1.5시간 "영어 회화"
 */
@Entity
@Table(name = "timetable_schedules")
@Getter
@Setter
@NoArgsConstructor
public class TimetableSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    // 요일: Mon, Tue, Wed, Thu, Fri, Sat, Sun
    @Column(name = "day_of_week", nullable = false, length = 3)
    private String day;

    // 시작 시각: "HH:MM"
    @Column(name = "start_time", nullable = false, length = 5)
    private String time;

    // 몇 시간 동안 (1.5 = 1시간 30분)
    @Column(nullable = false)
    private Double duration;

    @Column(length = 20)
    private String color;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}