package com.newyou.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.newyou.dto.ScheduleRequest;
import com.newyou.entity.User;
import com.newyou.service.TimetableScheduleService;

/**
 * 시간표 일정 API
 * GET /api/schedules 내 일정 목록
 * POST /api/schedules 일정 추가
 * PATCH /api/schedules/{id} 일정 수정
 * DELETE /api/schedules/{id} 일정 삭제
 */
@RestController
@RequestMapping("/api/schedules")
public class TimetableScheduleController {

    private final TimetableScheduleService service;

    public TimetableScheduleController(TimetableScheduleService service) {
        this.service = service;
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "로그인이 필요합니다."));
    }

    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal User user) {
        if (user == null)
            return unauthorized();
        return ResponseEntity.ok(service.getMySchedules(user.getId()));
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal User user, @RequestBody ScheduleRequest req) {
        if (user == null)
            return unauthorized();
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@AuthenticationPrincipal User user, @PathVariable Long id,
            @RequestBody ScheduleRequest req) {
        if (user == null)
            return unauthorized();
        try {
            return ResponseEntity.ok(service.update(id, user.getId(), req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        if (user == null)
            return unauthorized();
        try {
            service.delete(id, user.getId());
            return ResponseEntity.ok(Map.of("message", "일정이 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}