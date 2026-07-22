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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 20)
    private String color;

    // ⭐⭐ MakePlan에서 지정한 날짜 필드 (YYYY-MM-DD 형식으로 저장) ⭐⭐
    @Column(nullable = false, length = 10)
    private String planDate;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    // ⭐⭐ 빌더에 planDate 추가 ⭐⭐
    public Plan(String title, String content, String color, String planDate, User user) {
        this.title = title;
        this.content = content;
        this.color = color;
        this.planDate = planDate; // ⭐⭐ 필드 초기화 ⭐⭐
        this.user = user;
    }

    // ⭐⭐ update 메서드에 planDate 수정 로직 추가 ⭐⭐
    public void update(String title, String content, String color, String planDate) {
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
        if (color != null) {
            this.color = color;
        }
        if (planDate != null) {
            this.planDate = planDate;
        }
    }
}