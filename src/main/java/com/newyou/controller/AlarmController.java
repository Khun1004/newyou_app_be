package com.newyou.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.newyou.dto.AlarmDto;
import com.newyou.entity.User;
import com.newyou.service.AlarmService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/alarms")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    // 1. 알람 목록 조회 (GET)
    @GetMapping
    public ResponseEntity<?> getAlarms(@AuthenticationPrincipal User user) {
        try {
            log.info("GET /api/alarms 호출. User ID: {}", user.getId());

            List<AlarmDto> alarms = alarmService.getAlarmsByUserId(user.getId());

            return ResponseEntity.ok(alarms);
        } catch (Exception e) {
            log.error("알람 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "알람 목록 조회 중 오류가 발생했습니다."));
        }
    }

    // 2. 새로운 알람 생성 (POST)
    @PostMapping
    public ResponseEntity<?> createAlarm(
            @AuthenticationPrincipal User user,
            @RequestBody AlarmDto alarmDto) {

        try {
            log.info("POST /api/alarms 호출. User ID: {}", user.getId());
            log.debug("요청 데이터: {}", alarmDto.toString());

            // 필터링된 알람 정보로 서비스 호출
            AlarmDto createdAlarm = alarmService.createAlarm(user, alarmDto);

            return ResponseEntity.status(HttpStatus.CREATED).body(createdAlarm);
        } catch (IllegalArgumentException e) {
            log.warn("❌ 알람 생성 실패 (400 Bad Request): {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("❌ 알람 생성 실패 (500 Internal Error): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "알람 생성 중 서버 오류가 발생했습니다."));
        }
    }

    // 3. 알람 수정 (PUT)
    // 🚨 PUT 대신 PATCH를 사용하는 것이 더 일반적이지만, 여기서는 기존 알람 생성을 제외한
    // 모든 필드를 덮어쓰는 의미로 PUT을 사용하겠습니다.
    @PutMapping("/{id}")
    public ResponseEntity<?> updateAlarm(
            @AuthenticationPrincipal User user,
            @PathVariable("id") String alarmId,
            @RequestBody AlarmDto alarmDto) {

        try {
            log.info("PUT /api/alarms/{} 호출. User ID: {}", alarmId, user.getId());

            // DTO의 ID가 경로 변수의 ID와 일치하도록 보장
            alarmDto.setId(alarmId);

            AlarmDto updatedAlarm = alarmService.updateAlarm(user.getId(), alarmId, alarmDto);

            log.info("✅ 알람 수정 성공. ID: {}", updatedAlarm.getId());
            return ResponseEntity.ok(updatedAlarm);
        } catch (IllegalArgumentException e) {
            log.warn("❌ 알람 수정 실패 (404 Not Found / 403 Forbidden): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("❌ 알람 수정 실패 (500 Internal Error): {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "알람 수정 중 서버 오류가 발생했습니다."));
        }
    }

    // 4. 알람 활성화/비활성화 토글 (PATCH)
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleAlarm(
            @AuthenticationPrincipal User user,
            @PathVariable("id") String alarmId) {

        try {
            log.info("PATCH /api/alarms/{}/toggle 호출. User ID: {}", alarmId, user.getId());

            AlarmDto updatedAlarm = alarmService.toggleAlarm(user.getId(), alarmId);

            return ResponseEntity.ok(updatedAlarm);
        } catch (IllegalArgumentException e) {
            log.warn("❌ 알람 토글 실패 (404 Not Found / 403 Forbidden): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("❌ 알람 토글 실패 (500 Internal Error): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "알람 상태 변경 중 서버 오류가 발생했습니다."));
        }
    }

    // 5. 알람 삭제 (DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAlarm(
            @AuthenticationPrincipal User user,
            @PathVariable("id") String alarmId) {

        try {
            log.info("DELETE /api/alarms/{} 호출. User ID: {}", alarmId, user.getId());

            alarmService.deleteAlarm(user.getId(), alarmId);

            return ResponseEntity.ok(Map.of("message", "알람이 성공적으로 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            log.warn("❌ 알람 삭제 실패 (404 Not Found / 403 Forbidden): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", e.getMessage()));
        } catch (RuntimeException e) {
            log.error("❌ 알람 삭제 실패 (500 Internal Error): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "알람 삭제 중 서버 오류가 발생했습니다."));
        }
    }
}