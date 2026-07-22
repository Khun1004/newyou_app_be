package com.newyou.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "alarms")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Alarm {

    // 1. Primary Key (DB 컬럼 'pk'에 매핑됨, AUTO_INCREMENT)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long pk;

    // 2. FIX: DB의 필수 컬럼 'id'에 매핑되는 필드.
    // 이전에 값이 누락되어 'Field 'id' doesn't have a default value' 오류 발생.
    @Column(name = "id", nullable = false, unique = true)
    private String internalId;

    // 3. Client UUID (DB 컬럼 'client_id'에 매핑됨)
    @Column(name = "client_id", nullable = false, unique = true)
    private String id; // 클라이언트에서 생성된 UUID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 알람을 소유한 사용자

    @Column(nullable = false, length = 5)
    private String time; // "HH:mm" 형식 (예: "07:30")

    @Column(nullable = false, length = 100)
    private String label; // 알람 이름

    @JsonProperty("isActive") // 💡 JSON 직렬화 시 필드 이름을 강제합니다.
    private boolean isActive; // 활성화 상태

    @Column(length = 1024) // 🚨 voiceUri 컬럼 길이를 넉넉하게 확장 (기본 255자 초과 방지)
    private String voiceUri; // 녹음 파일 서버 경로 (상대 경로)

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "alarm_repeat_days", joinColumns = @JoinColumn(name = "alarm_pk"))
    @Column(name = "day_of_week")
    private List<String> repeat; // 반복 요일 목록 (예: ["Mon", "Wed", "Fri"])

    private String sound; // 알람 소리 이름/경로

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Entity가 저장되기 전에 실행되는 PrePersist 메서드
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        // 클라이언트 ID (UUID)가 없다면 새로 생성합니다.
        if (this.id == null || this.id.isEmpty()) {
            String newUuid = UUID.randomUUID().toString();
            this.id = newUuid;
            this.internalId = newUuid; // 💡 FIX: DB의 필수 컬럼 'id'에도 동일한 UUID를 할당합니다.
        } else {
            // 클라이언트 ID가 있다면, 내부 ID도 동일하게 설정합니다.
            this.internalId = this.id;
        }
    }

    // Entity가 업데이트되기 전에 실행되는 PreUpdate 메서드
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public Alarm(String id, User user, String time, String label, boolean isActive, String voiceUri,
            List<String> repeat, String sound) {
        // 클라이언트에서 넘어온 'id' 값으로 'this.id' (client_id 컬럼)을 설정합니다.
        this.id = id;

        // internalId (DB 'id' 컬럼)는 Builder에서 설정하지 않고 PrePersist에서 자동으로 설정됩니다.
        this.user = user;
        this.time = time;
        this.label = label;
        this.isActive = isActive;
        this.voiceUri = voiceUri;
        this.repeat = repeat;
        this.sound = sound;
    }
}