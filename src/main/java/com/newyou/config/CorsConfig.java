package com.newyou.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS(Cross-Origin Resource Sharing) 설정을 위한 클래스입니다.
 * React Native 앱과 같이 서버와 다른 도메인(IP, 포트)에서 API를 호출할 수 있도록 허용합니다.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 모든 경로(/**)에 대해 CORS를 허용하도록 설정
        registry.addMapping("/**")

                // AllowedOrigins: 모든 오리진 "*"을 허용합니다.
                // 개발 단계에서는 "*"를 사용하여 React Native 앱의 IP 주소에 관계없이 접근을 허용합니다.
                .allowedOrigins("*")

                // AllowedMethods: 모든 HTTP 메서드(GET, POST, PUT, DELETE 등)를 허용합니다.
                .allowedMethods("*")

                // AllowedHeaders: 모든 헤더를 허용합니다.
                .allowedHeaders("*");
    }
}