package com.newyou.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.newyou.entity.Plan;

public interface PlanRepository extends JpaRepository<Plan, Long> {
    // 특정 사용자(userId)의 모든 계획을 최신순으로 조회
    List<Plan> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 특정 사용자(userId)의 특정 계획(id)을 조회 (소유권 확인용)
    Optional<Plan> findByIdAndUserId(Long id, Long userId);
}