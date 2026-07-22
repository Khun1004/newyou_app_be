package com.newyou.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter; // 💡 import 추가
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.newyou.dto.FriendRequest;
import com.newyou.entity.Friend;
import com.newyou.repository.FriendRepository;

@Service
public class FriendService {

    private final FriendRepository friendRepository;

    @Autowired
    public FriendService(FriendRepository friendRepository) {
        this.friendRepository = friendRepository;
    }

    // 💡 String "MM-dd"를 LocalDate로 변환하는 Helper 메서드
    private LocalDate parseBirthdate(String birthdateStr) {
        if (birthdateStr == null || birthdateStr.trim().isEmpty()) {
            return null;
        }
        // 클라이언트에서 보낸 'MM-dd' 형식만 있다면, 파싱을 위해 더미 연도 '2000-'을 붙입니다.
        try {
            String dateStringWithYear = "2000-" + birthdateStr;
            // "2000-MM-dd" 형식으로 파싱합니다.
            return LocalDate.parse(dateStringWithYear, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            throw new IllegalArgumentException("생일 날짜 형식이 올바르지 않거나 유효하지 않은 날짜입니다. (MM-dd)", e);
        }
    }

    // 1. 친구 목록 조회 (생략)
    public List<Friend> getFriends(Long userId) {
        return friendRepository.findByUserId(userId);
    }

    // 2. 친구 상세 조회 (권한 확인 포함) (생략)
    public Friend getFriend(Long userId, Long friendId) {
        return friendRepository.findByUserIdAndId(userId, friendId)
                .orElseThrow(() -> new IllegalArgumentException("친구를 찾을 수 없거나 접근 권한이 없습니다."));
    }

    // 3. 친구 추가
    @Transactional
    public Friend addFriend(Long userId, FriendRequest request) {
        // 1. 닉네임 중복 검사 (같은 사용자 내에서)
        friendRepository.findByUserIdAndNickname(userId, request.getNickname()).ifPresent(f -> {
            throw new IllegalArgumentException("이미 사용 중인 친구 닉네임입니다.");
        });

        // 2. Friend 엔티티 생성
        Friend friend = Friend.builder()
                .userId(userId)
                .nickname(request.getNickname())
                .profileColor(request.getProfileColor())
                .profileImage(request.getProfileImage()) // 컨트롤러에서 설정된 URL
                .memo(request.getMemo())
                .build();

        // 💡 수정: String으로 받은 birthdate를 LocalDate로 변환하여 설정
        friend.setBirthdate(parseBirthdate(request.getBirthdate()));

        // 4. 저장 및 반환
        return friendRepository.save(friend);
    }

    // 4. 친구 정보 업데이트
    @Transactional
    public Friend updateFriend(Long userId, Long friendId, FriendRequest request) {
        Friend friend = getFriend(userId, friendId);

        // 1. 닉네임 업데이트
        if (!friend.getNickname().equals(request.getNickname())) {
            friendRepository.findByUserIdAndNickname(userId, request.getNickname()).ifPresent(f -> {
                throw new IllegalArgumentException("이미 사용 중인 친구 닉네임입니다.");
            });
            friend.setNickname(request.getNickname());
        }

        // 💡 수정: String으로 받은 birthdate를 LocalDate로 변환하여 설정
        friend.setBirthdate(parseBirthdate(request.getBirthdate()));

        // 3. 프로필 및 메모 업데이트
        friend.setProfileColor(request.getProfileColor());
        friend.setProfileImage(request.getProfileImage()); // 컨트롤러에서 설정된 URL
        friend.setMemo(request.getMemo());

        // 4. 저장 및 반환
        return friendRepository.save(friend);
    }

    // 5. 친구 삭제 (생략)
    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        Friend friend = getFriend(userId, friendId);
        friendRepository.delete(friend);
    }

    // 6. 오늘 생일인 친구 목록 조회 (생략)
    public List<Friend> getTodayBirthdays(Long userId) {
        int currentMonth = java.time.LocalDate.now().getMonthValue();
        List<Friend> monthlyBirthdays = friendRepository.findByUserIdAndBirthMonth(userId, currentMonth);

        int today = java.time.LocalDate.now().getDayOfMonth();
        return monthlyBirthdays.stream()
                .filter(f -> f.getBirthDay() != null && f.getBirthDay().equals(today))
                .toList();
    }
}