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

    /** 나를(linkedUserId) 친구로 연결해 둔 사람들의 친구 목록 → 그 사람들의 상태를 내가 볼 수 있어요 */
    List<Friend> findByLinkedUserId(Long linkedUserId);

    /** owner가 viewer를 연결해 두었는지 (상태를 볼 수 있는지) */
    boolean existsByUserIdAndLinkedUserId(Long userId, Long linkedUserId);

    /** 내 친구 목록에서 그 New You 계정과 연결된 친구 (아이디로 친구 추가할 때 중복 확인) */
    Optional<Friend> findFirstByUserIdAndLinkedUserId(Long userId, Long linkedUserId);
}
