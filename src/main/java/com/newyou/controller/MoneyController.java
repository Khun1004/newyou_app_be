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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.newyou.dto.MoneyRequest;
import com.newyou.entity.User;
import com.newyou.service.MoneyService;

/**
 * 가계부 API
 *   GET    /api/money?month=2026-10   그 달 기록 (month 없으면 전체)
 *   POST   /api/money                 기록 추가
 *   PATCH  /api/money/{id}            기록 수정
 *   DELETE /api/money/{id}            기록 삭제
 */
@RestController
@RequestMapping("/api/money")
public class MoneyController {

    private final MoneyService service;

    public MoneyController(MoneyService service) {
        this.service = service;
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인이 필요합니다."));
    }

    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal User user,
                                  @RequestParam(required = false) String month) {
        if (user == null) return unauthorized();
        return ResponseEntity.ok(service.getMyRecords(user.getId(), month));
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal User user, @RequestBody MoneyRequest req) {
        if (user == null) return unauthorized();
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(user, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> update(@AuthenticationPrincipal User user, @PathVariable Long id,
                                    @RequestBody MoneyRequest req) {
        if (user == null) return unauthorized();
        try {
            return ResponseEntity.ok(service.update(id, user.getId(), req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal User user, @PathVariable Long id) {
        if (user == null) return unauthorized();
        try {
            service.delete(id, user.getId());
            return ResponseEntity.ok(Map.of("message", "기록이 삭제되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}