package com.newyou.dto;

public class ProfileUpdateRequest {

    private String nickname;
    private String password;
    private String profileImage;

    // 기본 생성자
    public ProfileUpdateRequest() {
    }

    // 모든 필드를 받는 생성자
    public ProfileUpdateRequest(String nickname, String password, String profileImage) {
        this.nickname = nickname;
        this.password = password;
        this.profileImage = profileImage;
    }

    // Getter & Setter
    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    @Override
    public String toString() {
        return "ProfileUpdateRequest{" +
                "nickname='" + nickname + '\'' +
                ", password='" + (password != null && !password.isEmpty() ? "***" : "null") + '\'' +
                ", profileImage='" + (profileImage != null ? "Base64 데이터 있음" : "null") + '\'' +
                '}';
    }
}