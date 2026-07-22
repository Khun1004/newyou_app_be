package com.newyou.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor; // @Builder와 함께 사용하기 위해 추가
import lombok.Builder; // FriendDto에서 엔티티 변환 시 사용
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 친구 정보(생일, 프로필 색상 등)를 저장하는 엔티티입니다.
 * user_id를 통해 특정 사용자의 친구임을 식별합니다.
 * 
 * @Getter/@Setter/@Builder를 통해 DTO에서 필요로 하는 모든 접근 메서드를 제공합니다.
 */
@Entity
@Table(name = "friends", indexes = {
        // userId와 birthMonth로 인덱스를 설정하여 특정 사용자의 월별 친구 목록 조회를 빠르게 합니다.
        @Index(name = "idx_user_month", columnList = "userId, birthMonth")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor // Lombok Builder 사용을 위해 추가
@Builder // FriendDto에서 사용
public class Friend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FriendResponse에서 getProfileColor()를 호출하는 필드
    private String profileColor;

    // 친구 정보를 소유한 사용자 ID (외래 키 역할)
    @Column(nullable = false)
    private Long userId;

    // 친구 닉네임
    @Column(nullable = false, length = 50)
    private String nickname;

    // 친구 생년월일 (날짜만 저장, 시간 정보는 필요 없음)
    @JsonFormat(pattern = "MM-dd")
    private LocalDate birthdate;

    // 월별 조회 편의성을 위해 생일 '월'을 별도로 저장합니다.
    @Column(nullable = true)
    private Integer birthMonth;

    // 생일 '일(Day)'을 별도로 저장합니다. (DTO에서 getBirthDay()를 기대하므로 추가)
    @Column(nullable = true)
    private Integer birthDay;

    // 프로필 이미지 URL 또는 Base64 필드 이름을 profileImage로 변경하여
    // DTO에서 예상하는 getProfileImage() 메서드가 생성되도록 합니다.
    @Column(length = 500)
    private String profileImage;

    // 메모
    @Column(length = 1000)
    private String memo;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    /**
     * birthdate 설정 시 birthMonth와 birthDay도 자동으로 설정합니다.
     * DTO에서 기대하는 getBirthDay() 메서드를 제공하기 위해 birthDay 필드에 값을 할당합니다.
     */
    public void setBirthdate(LocalDate birthdate) {
        this.birthdate = birthdate;
        if (birthdate != null) {
            this.birthMonth = birthdate.getMonthValue();
            this.birthDay = birthdate.getDayOfMonth(); // <<-- 생일 '일' 정보 추가
        } else {
            this.birthMonth = null;
            this.birthDay = null; // <<-- 생일 '일' 정보 초기화
        }
    }
}