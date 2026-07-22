package com.newyou.dto;

import java.util.List;

import com.newyou.entity.Alarm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlarmDto {

    // 클라이언트에서 보낼 때는 null이거나 UUID, 받을 때는 필수
    private String id;

    // 필수 입력 필드
    private String time; // "HH:mm"
    private String label;
    private boolean isActive;

    // 선택 필드
    private String voiceUri; // Base64 인코딩된 음성 파일 데이터 또는 기존 경로
    private List<String> repeat; // 요일 목록 (Mon, Tue, ...)
    private String sound;

    // Entity -> DTO 변환
    public static AlarmDto fromEntity(Alarm alarm) {
        return AlarmDto.builder()
                .id(alarm.getId())
                .time(alarm.getTime())
                .label(alarm.getLabel())
                .isActive(alarm.isActive())
                .voiceUri(alarm.getVoiceUri())
                .repeat(alarm.getRepeat())
                .sound(alarm.getSound())
                .build();
    }
}