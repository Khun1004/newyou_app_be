package com.newyou.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.newyou.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 전화번호로 사용자 찾기
    Optional<User> findByPhoneNumber(String phoneNumber);

    // 닉네임으로 사용자 찾기 (중복 체크용)
    Optional<User> findByName(String name);

    // 전화번호 존재 여부 확인
    boolean existsByPhoneNumber(String phoneNumber);

    // 닉네임 존재 여부 확인
    boolean existsByName(String name);

    // 아이디로 사용자 찾기 / 아이디 중복 확인
    Optional<User> findByLoginId(String loginId);

    boolean existsByLoginId(String loginId);

    // 🌱 뉴유 친구 찾기: 최근 가입한 사람 / 이름·아이디로 찾기 (아이디가 있는 사람만)
    List<User> findTop100ByLoginIdIsNotNullOrderByCreatedAtDesc();

    List<User> findTop50ByLoginIdContainingIgnoreCaseOrNameContainingIgnoreCase(String loginId, String name);
}