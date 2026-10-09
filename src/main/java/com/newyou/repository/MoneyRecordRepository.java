package com.newyou.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.newyou.entity.MoneyRecord;

public interface MoneyRecordRepository extends JpaRepository<MoneyRecord, Long> {
    // 내 기록 전체 (최근 날짜부터)
    List<MoneyRecord> findByUserIdOrderByDateDescIdDesc(Long userId);

    // 내 기록 중 특정 달 ("2026-10" 으로 시작하는 날짜)
    List<MoneyRecord> findByUserIdAndDateStartingWithOrderByDateDescIdDesc(Long userId, String yearMonth);

    // 내 기록인지 확인하면서 하나 찾기
    Optional<MoneyRecord> findByIdAndUserId(Long id, Long userId);
}