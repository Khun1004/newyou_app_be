package com.newyou.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.newyou.entity.Friend;

@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {

    /**
     * 특정 사용자의 모든 친구 목록을 조회합니다.
     * 
     * @param userId 사용자 ID
     * @return 친구 목록
     */
    List<Friend> findByUserId(Long userId);

    /**
     * 특정 사용자의 특정 월에 생일인 친구 목록을 조회합니다.
     * 
     * @param userId     사용자 ID
     * @param birthMonth 생일 월 (1-12)
     * @return 해당 월의 생일 친구 목록
     */
    List<Friend> findByUserIdAndBirthMonth(Long userId, Integer birthMonth);

    /**
     * 특정 사용자 ID와 친구 ID로 친구를 조회합니다. (권한 확인용)
     * 
     * @param userId 사용자 ID
     * @param id     친구 ID
     * @return Friend 객체 Optional
     */
    Optional<Friend> findByUserIdAndId(Long userId, Long id);

    /**
     * 특정 사용자의 친구 닉네임 중복을 확인합니다.
     * 
     * @param userId   사용자 ID
     * @param nickname 친구 닉네임
     * @return Friend 객체 Optional
     */
    Optional<Friend> findByUserIdAndNickname(Long userId, String nickname);
}