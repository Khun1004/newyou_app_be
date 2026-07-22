package com.newyou.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.newyou.dto.PlanRequest;
import com.newyou.dto.PlanResponse;
import com.newyou.entity.Plan;
import com.newyou.entity.User;
import com.newyou.repository.PlanRepository;

@Service
public class PlanService {

    private final PlanRepository planRepository;

    @Autowired
    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    // ----------------------------------------------------
    // 1. 새로운 계획 생성 (C: Create)
    // ----------------------------------------------------
    @Transactional
    public PlanResponse createPlan(User user, PlanRequest request) {
        if (request.getTitle() == null || request.getTitle().isEmpty()) {
            throw new IllegalArgumentException("계획의 제목은 필수입니다.");
        }
        if (request.getContent() == null || request.getContent().isEmpty()) {
            throw new IllegalArgumentException("계획의 내용은 필수입니다.");
        }
        // ⭐⭐ planDate 필수 체크 추가 ⭐⭐
        if (request.getPlanDate() == null || request.getPlanDate().isEmpty()) {
            throw new IllegalArgumentException("계획 날짜(planDate)는 필수입니다.");
        }

        Plan newPlan = Plan.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .color(request.getColor() != null ? request.getColor() : "#FFFFFF") // 기본 색상 설정
                .planDate(request.getPlanDate()) // ⭐⭐ planDate 빌더에 추가 ⭐⭐
                .user(user)
                .build();

        Plan savedPlan = planRepository.save(newPlan);
        return new PlanResponse(savedPlan);
    }

    // ----------------------------------------------------
    // 2. 모든 계획 조회 (R: Read)
    // ----------------------------------------------------
    @Transactional(readOnly = true)
    public List<PlanResponse> getPlansByUserId(Long userId) {
        // 기존 로직 유지: userId로 조회 후 PlanResponse로 변환
        List<Plan> plans = planRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return plans.stream()
                .map(PlanResponse::new)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------
    // 3. 계획 수정 (U: Update)
    // ----------------------------------------------------
    @Transactional
    public PlanResponse updatePlan(Long planId, Long userId, PlanRequest request) {
        Plan plan = planRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 접근 권한이 없는 계획입니다."));

        // ⭐⭐ plan.update() 메서드에 planDate 인자를 추가하여 호출 ⭐⭐
        // PlanRequest의 getter를 사용하려면 PlanRequest.java에 getPlanDate()가 필요합니다.
        plan.update(
                request.getTitle(),
                request.getContent(),
                request.getColor(),
                request.getPlanDate() // ⭐⭐ 오류를 해결하기 위해 planDate 추가 ⭐⭐
        );

        // save()를 호출할 필요 없이, @Transactional 덕분에 변경 내용이 자동으로 DB에 반영됩니다 (Dirty Checking).
        return new PlanResponse(plan);
    }

    // ----------------------------------------------------
    // 4. 계획 삭제 (D: Delete)
    // ----------------------------------------------------
    @Transactional
    public void deletePlan(Long planId, Long userId) {
        Plan plan = planRepository.findByIdAndUserId(planId, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 접근 권한이 없는 계획입니다."));

        planRepository.delete(plan);
    }
}