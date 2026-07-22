package com.newyou.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");

        // 🔍 상세 로깅 시작
        log.info("========================================");
        log.info("JWT 필터 실행");
        log.info("요청 URI: {}", requestURI);
        log.info("요청 메서드: {}", request.getMethod());
        log.info("Authorization 헤더: {}",
                authHeader != null ? authHeader.substring(0, Math.min(30, authHeader.length())) + "..." : "없음");

        // 1. Request Header에서 JWT 토큰 추출
        String token = jwtTokenProvider.resolveToken(request);
        log.info("추출된 토큰: {}", token != null ? token.substring(0, Math.min(20, token.length())) + "..." : "없음");

        // 2. 토큰 유효성 검사
        if (token != null && jwtTokenProvider.validateToken(token)) {
            // 3. 토큰이 유효하면 인증 객체(Authentication) 생성
            try {
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                // 4. SecurityContext에 Authentication 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("✅ 인증 성공: 사용자 ID: {}", authentication.getName());
                log.info("인증 권한: {}", authentication.getAuthorities());
            } catch (Exception e) {
                log.error("❌ 인증 처리 중 오류 발생: {}", e.getMessage());
                e.printStackTrace();
            }
        } else {
            if (token == null) {
                log.warn("⚠️ 토큰이 없습니다.");
            } else {
                log.warn("⚠️ 토큰 검증 실패: 유효하지 않은 토큰");
            }
        }

        log.info("========================================");

        filterChain.doFilter(request, response);
    }
}