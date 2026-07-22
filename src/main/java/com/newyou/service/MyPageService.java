package com.newyou.service;

import org.springframework.stereotype.Service;

import com.newyou.dto.MyPageResponse;
import com.newyou.entity.User;

/**
 * 마이페이지 관련 비즈니스 로직을 처리하는 서비스입니다.
 */
@Service
public class MyPageService {

    /**
     * 사용자 정보 및 통계 데이터를 조회하여 DTO로 변환합니다.
     * * @param user 현재 인증된 사용자 엔티티
     * 
     * @return MyPageResponse DTO
     */
    public MyPageResponse getMyPageData(User user) {

        // 💡 실제 로직에서는 User 엔티티의 ID를 사용하여
        // 다른 테이블 (계획, 알람, 기념일, 사용 로그 등)에서 데이터를 조회해야 합니다.

        // --- Mock 통계 데이터 (MyScreen.tsx에 하드코딩된 값 사용) ---
        int completedPlans = 24;
        int alarmCount = 2; // alarms.length를 대신하여 2를 사용
        int anniversaries = 8;
        int consecutiveDays = 15;

        // User 엔티티와 통계 데이터를 결합하여 DTO 생성
        return MyPageResponse.fromUserAndStats(
                user,
                completedPlans,
                alarmCount,
                anniversaries,
                consecutiveDays);
    }
}