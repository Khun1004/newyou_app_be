package com.newyou.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.newyou.dto.PlanRequest;
import com.newyou.dto.PlanResponse;
import com.newyou.entity.User;
import com.newyou.service.PlanService;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    private final PlanService planService;

    @Autowired
    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    // 인증 확인 메서드 (UserController 참조)
    private ResponseEntity<?> checkAuthentication(User user) {
        if (user == null) {
            System.err.println("ERROR: 인증되지 않은 사용자의 접근 시도");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "인증되지 않은 사용자입니다. 다시 로그인 해주세요."));
        }
        return null; // 인증 성공
    }

    /**
     * 새 계획 생성 (POST /api/plans)
     */
    @PostMapping
    public ResponseEntity<?> createPlan(
            @AuthenticationPrincipal User user,
            @RequestBody PlanRequest request) {

        ResponseEntity<?> authCheck = checkAuthentication(user);
        if (authCheck != null)
            return authCheck;

        try {
            PlanResponse response = planService.createPlan(user, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * 사용자 본인의 모든 계획 조회 (GET /api/plans)
     */
    @GetMapping
    public ResponseEntity<?> getMyPlans(@AuthenticationPrincipal User user) {

        ResponseEntity<?> authCheck = checkAuthentication(user);
        if (authCheck != null)
            return authCheck;

        try {
            List<PlanResponse> plans = planService.getPlansByUserId(user.getId());
            return ResponseEntity.ok(plans);

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "계획 목록 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 특정 계획 수정 (PATCH /api/plans/{planId})
     * React의 updatePlan 기능을 지원합니다.
     */
    @PatchMapping("/{planId}")
    public ResponseEntity<?> updatePlan(
            @AuthenticationPrincipal User user,
            @PathVariable Long planId,
            @RequestBody PlanRequest request) {

        ResponseEntity<?> authCheck = checkAuthentication(user);
        if (authCheck != null)
            return authCheck;

        try {
            PlanResponse response = planService.updatePlan(planId, user.getId(), request);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // 소유권 없음 또는 계획 ID 오류
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "계획 수정 중 서버 오류가 발생했습니다."));
        }
    }

    /**
     * 특정 계획 삭제 (DELETE /api/plans/{planId})
     * React의 deletePlan 기능을 지원합니다.
     */
    @DeleteMapping("/{planId}")
    public ResponseEntity<?> deletePlan(
            @AuthenticationPrincipal User user,
            @PathVariable Long planId) {

        ResponseEntity<?> authCheck = checkAuthentication(user);
        if (authCheck != null)
            return authCheck;

        try {
            planService.deletePlan(planId, user.getId());
            return ResponseEntity.ok(Map.of("message", "계획이 성공적으로 삭제되었습니다."));

        } catch (IllegalArgumentException e) {
            // 소유권 없음 또는 계획 ID 오류
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "계획 삭제 중 서버 오류가 발생했습니다."));
        }
    }
}