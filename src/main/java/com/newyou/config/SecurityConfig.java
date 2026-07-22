package com.newyou.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.newyou.security.JwtAuthenticationFilter;
import com.newyou.security.JwtTokenProvider;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
        log.info("========================================");
        log.info("SecurityConfig 초기화 완료");
        log.info("========================================");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("========================================");
        log.info("Security Filter Chain 설정 시작");
        log.info("========================================");

        http
                // 1. CORS 설정 (CorsConfig에서 이미 설정, 여기서는 비활성화)
                .cors(cors -> cors.disable())
                // 2. CSRF 보호 비활성화 (JWT 사용 시 일반적으로 필요 없음)
                .csrf(csrf -> csrf.disable())

                // 3. 인가 규칙 설정
                .authorizeHttpRequests(auth -> {
                    log.info("인가 규칙 설정:");
                    log.info("- /api/auth/** : 인증 불필요");
                    log.info("- /uploads/profiles/**, /uploads/voices/** : 인증 불필요 (정적 리소스)");
                    log.info("- /uploads/friendprofiles/** : 인증 불필요 (정적 리소스) 💡 추가됨"); // 💡 추가됨
                    log.info("- /api/users/** : 인증 필요");
                    log.info("- /api/alarms/** : 인증 필요");
                    log.info("- /api/friends/** : 인증 필요");
                    log.info("- 기타 모든 요청 : 인증 필요");

                    auth
                            .requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers("/uploads/profiles/**").permitAll()
                            .requestMatchers("/uploads/voices/**").permitAll()
                            // 🚨 수정: 친구 프로필 경로에 대한 permitAll() 추가
                            .requestMatchers("/uploads/friendprofiles/**").permitAll()
                            .requestMatchers("/api/users/**").authenticated()
                            .requestMatchers("/api/alarms/**").authenticated()
                            .requestMatchers("/api/friends/**").authenticated()
                            .anyRequest().authenticated();
                })

                // 4. 세션 관리 (스테이트리스)
                .sessionManagement(session -> {
                    log.info("세션 관리: STATELESS (JWT 기반)");
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                })

                // 5. 기본 폼 로그인/HTTP Basic 비활성화
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // 6. JWT 인증 필터 추가
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        log.info("========================================");
        log.info("Security Filter Chain 설정 완료!");
        log.info("========================================");

        return http.build();
    }
}