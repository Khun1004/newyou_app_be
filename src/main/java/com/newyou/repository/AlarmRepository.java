package com.newyou.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.newyou.entity.Alarm;

@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long> {

    // 사용자 ID로 모든 알람 목록을 조회 (MyPage 등에서 사용)
    List<Alarm> findByUserId(Long userId);

    // 클라이언트 ID(UUID)로 특정 알람 조회 (수정/삭제 시 사용)
    Optional<Alarm> findById(String id);

    // ✅ 사용자 ID와 클라이언트 ID(UUID)로 특정 알람 조회 (보안 강화)
    Optional<Alarm> findByUserIdAndId(Long userId, String id);

    // 클라이언트 ID(UUID)로 삭제
    void deleteById(String id);
}