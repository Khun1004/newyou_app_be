package com.newyou.entity;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 15)
    private String phoneNumber;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // 요청 시에만 받고, 응답에는 포함하지 않음
    @Column(nullable = false)
    private String password; // 암호화된 비밀번호

    @Column(unique = true, nullable = false, length = 50)
    private String name; // 닉네임

    @Column(length = 500)
    private String profileImage;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // ----------------------------------------------------
    // UserDetails 인터페이스 구현 메서드
    // ----------------------------------------------------

    @JsonIgnore // JSON 응답에서 제외
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @JsonIgnore // JSON 응답에서 제외 (getPassword는 이미 @JsonProperty로 처리되지만 명시적으로 추가)
    @Override
    public String getPassword() {
        return this.password;
    }

    @JsonIgnore // JSON 응답에서 제외
    @Override
    public String getUsername() {
        // JWT Subject로 사용하기 위해 'phoneNumber'를 문자열로 반환
        return this.phoneNumber;
    }

    @JsonIgnore // JSON 응답에서 제외
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @JsonIgnore // JSON 응답에서 제외
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @JsonIgnore // JSON 응답에서 제외
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @JsonIgnore // JSON 응답에서 제외
    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", name='" + name + '\'' +
                ", profileImage='" + profileImage + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}