package com.newyou.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.newyou.repository.UserRepository;

/**
 * JWT의 Subject(사용자 전화번호)를 기반으로 DB에서 User 객체를 로드하는 서비스
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // JWT에서 추출한 Subject(사용자 전화번호)를 loadUserByUsername으로 받습니다.
    @Override
    public UserDetails loadUserByUsername(String phoneNumber) throws UsernameNotFoundException {
        // JWT의 Subject가 실제로는 '전화번호'이므로, 전화번호로 DB에서 조회합니다.
        // UserRepository.java에 findByPhoneNumber가 정의되어 있습니다.

        // User 엔티티가 UserDetails 인터페이스를 구현하고 있다고 가정합니다.
        return userRepository.findByPhoneNumber(phoneNumber)
                // 💡 수정: ID 대신 전화번호로 조회
                .orElseThrow(() -> new UsernameNotFoundException("User not found with phone number: " + phoneNumber));
        // 💡 예외 메시지도 전화번호 기반으로 변경
    }
}