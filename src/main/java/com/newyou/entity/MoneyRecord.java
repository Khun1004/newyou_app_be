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
 * 가계부 기록 (수입 또는 지출 한 건)
 * 예) 2026-10-10 지출 식비 8,000원 "점심 김밥"
 */
@Entity
@Table(name = "money_records")
@Getter
@Setter
@NoArgsConstructor
public class MoneyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // INCOME(수입) 또는 EXPENSE(지출)
    @Column(nullable = false, length = 10)
    private String type;

    // 금액 (원)
    @Column(nullable = false)
    private Long amount;

    // 분류 (식비, 교통, 월급 ...)
    @Column(nullable = false, length = 30)
    private String category;

    // 메모 (선택)
    @Column(length = 200)
    private String memo;

    // 날짜 "YYYY-MM-DD"
    @Column(name = "record_date", nullable = false, length = 10)
    private String date;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}