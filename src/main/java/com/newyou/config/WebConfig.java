package com.newyou.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:./uploads/profiles}")
    private String uploadDir; // 사용자 프로필 경로

    @Value("${file.upload-dir.voice:./uploads/voices}")
    private String voiceUploadDir; // 음성 파일 경로

    @Value("${file.upload-dir.friend-profile:./uploads/friendprofiles}")
    private String friendProfileUploadDir; // 친구 프로필 경로

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. 사용자 프로필 파일 핸들러 (기존)
        registry.addResourceHandler("/uploads/profiles/**")
                .addResourceLocations("file:" + uploadDir + "/")
                .setCachePeriod(3600);

        // 2. 음성 파일 핸들러 (기존)
        registry.addResourceHandler("/uploads/voices/**")
                .addResourceLocations("file:" + voiceUploadDir + "/")
                .setCachePeriod(3600);

        // 3. ✨ 수정: 친구 프로필 이미지 파일 핸들러
        // 클라이언트가 원하는 경로인 /uploads/friendprofiles/** 로 직접 접근하도록 수정합니다.
        registry.addResourceHandler("/uploads/friendprofiles/**") // 💡 /api/ 경로 제거
                .addResourceLocations("file:" + friendProfileUploadDir + "/")
                .setCachePeriod(3600);

        log.info("========================================");
        log.info("정적 리소스 핸들러 설정 완료");
        log.info("친구 프로필 디렉토리: {}", friendProfileUploadDir);
        log.info("접근 URL: /uploads/friendprofiles/**"); // 💡 /api/ 제거된 경로
        log.info("========================================");
    }

    // CORS 설정은 기존과 동일하게 유지
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(false)
                .maxAge(3600);

        log.info("✅ CORS 설정 완료 (모든 오리진 허용)");
    }
}