package com.newyou.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter; // 💡 import 추가
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.newyou.dto.FriendRequest;
import com.newyou.entity.Friend;
import com.newyou.entity.User;
import com.newyou.repository.FriendRepository;
import com.newyou.repository.UserRepository;

@Service
public class FriendService {

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;
    private final PrivacyService privacyService;

    @Autowired
    public FriendService(FriendRepository friendRepository, UserRepository userRepository, PrivacyService privacyService) {
        this.friendRepository = friendRepository;
        this.userRepository = userRepository;
        this.privacyService = privacyService;
    }

    // =======================================================================
    // 🔍 아이디로 New You 사용자 찾기 / 친구 추가
    //   - New You 앱을 쓰는 사람만 친구로 추가할 수 있어요
    // =======================================================================

    private User findUserByLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("친구 아이디를 입력해 주세요.");
        }
        String id = loginId.trim().toLowerCase().replaceFirst("^@", "");
        return userRepository.findByLoginId(id)
                .orElseThrow(() -> new IllegalArgumentException("'" + id + "' 아이디를 쓰는 New You 사용자가 없어요."));
    }

    /** GET /api/friends/search?loginId=xxx → 찾은 사람 정보 + 이미 친구인지 */
    @Transactional(readOnly = true)
    public Map<String, Object> searchByLoginId(Long me, String loginId) {
        User target = findUserByLoginId(loginId);
        if (privacyService.isBlocked(me, target.getId())) {
            // 차단 관계면 찾을 수 없는 것처럼 보여요
            throw new IllegalArgumentException("'" + target.getLoginId() + "' 아이디를 쓰는 New You 사용자가 없어요.");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("userId", target.getId());
        m.put("loginId", target.getLoginId());
        m.put("name", target.getName());
        m.put("profileImage", target.getProfileImage());
        m.put("isMe", target.getId().equals(me));
        Friend existing = friendRepository.findFirstByUserIdAndLinkedUserId(me, target.getId()).orElse(null);
        m.put("alreadyFriend", existing != null);
        m.put("friendId", existing != null ? existing.getId() : null);
        // 상대도 나를 친구로 추가했는지 (서로 친구)
        m.put("addedMe", friendRepository.existsByUserIdAndLinkedUserId(target.getId(), me));
        return m;
    }

    /**
     * GET /api/friends/discover?q=xxx → 🌱 New You 사용자 목록 (친구 추가용)
     *  - 나와 이미 친구인 사람(아이디로 연결된 사람)은 빼요
     *  - 나를 친구로 추가한 사람이 맨 위에 나와요
     *  - q 가 있으면 이름·아이디로 찾아요
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> discover(Long me, String q) {
        Set<Long> already = new HashSet<>();
        friendRepository.findByUserId(me).forEach(f -> {
            if (f.getLinkedUserId() != null) already.add(f.getLinkedUserId());
        });
        Set<Long> blocked = privacyService.blockedWith(me);
        Set<Long> addedMe = new HashSet<>();
        friendRepository.findByLinkedUserId(me).forEach(f -> addedMe.add(f.getUserId()));

        String query = q == null ? "" : q.trim().replaceFirst("^@", "");
        List<User> users = query.isEmpty()
                ? userRepository.findTop100ByLoginIdIsNotNullOrderByCreatedAtDesc()
                : userRepository.findTop50ByLoginIdContainingIgnoreCaseOrNameContainingIgnoreCase(query, query);

        List<Map<String, Object>> out = new ArrayList<>();
        for (User u : users) {
            if (u.getId().equals(me) || already.contains(u.getId()) || u.getLoginId() == null || blocked.contains(u.getId())) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", u.getId());
            m.put("loginId", u.getLoginId());
            m.put("name", u.getName());
            m.put("profileImage", u.getProfileImage());
            m.put("addedMe", addedMe.contains(u.getId()));
            out.add(m);
        }
        // 나를 추가한 사람 먼저
        out.sort((a, b) -> Boolean.compare(Boolean.TRUE.equals(b.get("addedMe")), Boolean.TRUE.equals(a.get("addedMe"))));
        return out;
    }

    /** POST /api/friends/add-by-id { loginId } → 아이디로 연결된 친구를 만들어요 */
    @Transactional
    public Friend addByLoginId(Long me, String loginId) {
        User target = findUserByLoginId(loginId);
        if (target.getId().equals(me)) {
            throw new IllegalArgumentException("나 자신은 친구로 추가할 수 없어요.");
        }
        if (privacyService.isBlocked(me, target.getId())) {
            throw new IllegalArgumentException("차단 관계인 사용자는 친구로 추가할 수 없어요. (기념일 관리 → 설정에서 차단을 풀 수 있어요)");
        }
        if (friendRepository.findFirstByUserIdAndLinkedUserId(me, target.getId()).isPresent()) {
            throw new IllegalArgumentException("이미 친구예요.");
        }

        String name = target.getName() != null && !target.getName().isBlank() ? target.getName() : target.getLoginId();

        // 같은 이름의 (아직 연결 안 된) 친구가 있으면 새로 만들지 않고 그 친구에 연결해요
        Friend sameName = friendRepository.findByUserIdAndNickname(me, name).orElse(null);
        if (sameName != null && sameName.getLinkedUserId() == null) {
            sameName.setLinkedUserId(target.getId());
            sameName.setLinkedLoginId(target.getLoginId());
            if (sameName.getProfileImage() == null && target.getProfileImage() != null) {
                sameName.setProfileImage(target.getProfileImage());
            }
            return friendRepository.save(sameName);
        }
        String nickname = sameName == null ? name : name + " (@" + target.getLoginId() + ")";
        if (nickname.length() > 50) nickname = nickname.substring(0, 50);

        Friend friend = Friend.builder()
                .userId(me)
                .nickname(nickname)
                .profileImage(target.getProfileImage())
                .linkedUserId(target.getId())
                .linkedLoginId(target.getLoginId())
                .build();
        return friendRepository.save(friend);
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