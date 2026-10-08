package com.newyou.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.newyou.entity.TimetableSchedule;

public interface TimetableScheduleRepository extends JpaRepository<TimetableSchedule, Long> {
    // 내 시간표 일정 전체
    List<TimetableSchedule> findByUserIdOrderByDayAscTimeAsc(Long userId);

    // 내 일정인지 확인하면서 하나 찾기
    Optional<TimetableSchedule> findByIdAndUserId(Long id, Long userId);
}