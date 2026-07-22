package com.newyou.service;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.newyou.dto.ProfileUpdateRequest;
import com.newyou.entity.User;
import com.newyou.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // application.properties에서 설정: file.upload-dir=C:/newyou_uploads/profiles
    @Value("${file.upload-dir:./uploads/profiles}")
    private String uploadDir;

    // 인메모리 인증 코드 저장소 (실제 서비스에서는 Redis/DB를 사용해야 함)
    private final Map<String, VerificationInfo> verificationCodes = new HashMap<>();

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private static class VerificationInfo {
        String code;
        long expiryTime;

        public VerificationInfo(String code) {
            this.code = code;
            this.expiryTime = System.currentTimeMillis() + 5 * 60 * 1000; // 5분
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    // =======================================================================
    // 1. 휴대폰 인증 로직 (AuthController 사용)
    // =======================================================================

    /**
     * 휴대폰 인증 코드를 생성하고 저장합니다. (실제로는 SMS 전송 로직이 필요)
     */
    public void sendVerificationCode(String phoneNumber) {
        // 6자리 랜덤 인증번호 생성
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));

        // 인메모리 저장소에 저장 (5분 만료)
        verificationCodes.put(phoneNumber, new VerificationInfo(code));

        // 💡 실제로는 여기서 외부 SMS API를 호출하여 사용자에게 코드를 전송합니다.
        // 현재는 콘솔에 출력하여 테스트용으로 사용합니다.
        System.out.println("[SMS MOCK] " + phoneNumber + "로 인증번호 발송: " + code);
    }

    /**
     * 휴대폰 인증 코드를 검증합니다.
     * 
     * @throws RuntimeException 인증번호가 없거나 만료된 경우
     */
    public boolean verifyCode(String phoneNumber, String code) {
        VerificationInfo info = verificationCodes.get(phoneNumber);

        if (info == null) {
            throw new RuntimeException("인증번호를 먼저 요청해 주세요.");
        }

        if (info.isExpired()) {
            verificationCodes.remove(phoneNumber); // 만료된 코드 제거
            throw new RuntimeException("인증 시간이 만료되었습니다. 다시 요청해 주세요.");
        }

        if (info.code.equals(code)) {
            verificationCodes.remove(phoneNumber); // 성공 시 코드 제거
            return true;
        }

        return false;
    }

    // =======================================================================
    // 2. 인증/회원가입 로직 (AuthController 사용)
    // =======================================================================

    public boolean isPhoneNumberExists(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber).isPresent();
    }

    @Transactional
    public User registerUser(User user) {
        // 전화번호 중복 검사는 Controller 또는 이전 단계에서 처리했다고 가정

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // 닉네임 중복 검사 (혹시 모를 우발적 중복 방지)
        userRepository.findByName(user.getName()).ifPresent(existingUser -> {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        });

        return userRepository.save(user);
    }

    public User authenticate(String phoneNumber, String password) {
        Optional<User> userOptional = userRepository.findByPhoneNumber(phoneNumber);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // 비밀번호 검증
            if (passwordEncoder.matches(password, user.getPassword())) {
                return user; // 인증 성공
            }
        }
        return null; // 인증 실패
    }

    // =======================================================================
    // 3. 프로필 업데이트 로직 (UserController 사용)
    // =======================================================================

    /**
     * Base64 문자열을 디코딩하여 서버 파일 시스템에 저장하고, 저장된 경로를 반환합니다.
     * 
     * @param base64Image "data:image/png;base64,..." 형식의 문자열
     * @return 저장된 파일의 공용 접근 경로 (예: /uploads/profiles/UUID.png)
     * @throws IllegalArgumentException Base64 형식 오류 시
     * @throws RuntimeException         이미지 저장 중 IO 오류 시
     */
    private String saveBase64Image(String base64Image) {
        String[] parts = base64Image.split(",");
        if (parts.length < 2) {
            throw new IllegalArgumentException("올바른 Base64 이미지 형식이 아닙니다.");
        }
        String base64Data = parts[1];
        String mimeTypePart = parts[0]; // data:image/jpeg;base64

        // 파일 확장자 결정 (간단화)
        String extension = ".jpg";
        if (mimeTypePart.contains("png")) {
            extension = ".png";
        } else if (mimeTypePart.contains("gif")) {
            extension = ".gif";
        }

        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            Path directoryPath = Paths.get(uploadDir);

            // 디렉토리가 없으면 생성
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
                System.out.println("LOG: 이미지 업로드 디렉토리 생성 완료: " + uploadDir);
            }

            // 고유한 파일 이름 생성
            String fileName = UUID.randomUUID().toString() + extension;
            Path filePath = directoryPath.resolve(fileName);

            // 파일 저장
            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                fos.write(imageBytes);
            }

            // DB에 저장할 상대 경로 반환 (WebConfig에서 /uploads/profiles/** 로 매핑되어야 함)
            // 'uploadDir'이 './uploads/profiles'라면, 저장 경로는 '/uploads/profiles/UUID.jpg'가 되어야
            // 합니다.
            // 여기서는 환경 설정에 따라 상대 경로를 반환합니다.
            String relativePath = "/uploads/profiles/" + fileName;
            return relativePath;

        } catch (Exception e) {
            System.err.println("FATAL ERROR (I/O EXCEPTION): " + e.getClass().getName() + ": " + e.getMessage());
            throw new RuntimeException("Base64 디코딩 및 파일 저장 중 오류 발생", e);
        }
    }

    @Transactional
    public User updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 1. 닉네임 업데이트
        if (request.getNickname() != null && !request.getNickname().isEmpty()) {
            if (request.getNickname().length() < 2 || request.getNickname().length() > 20) {
                throw new IllegalArgumentException("닉네임은 2-20자 이내로 입력해야 합니다.");
            }

            // 현재 닉네임과 다를 경우에만 중복 검사
            if (!request.getNickname().equals(user.getName())) {
                // [LOG] 닉네임 변경 시도 로깅
                System.out.println("LOG: 닉네임 변경 시도. 이전: " + user.getName() + ", 새: " + request.getNickname());

                userRepository.findByName(request.getNickname()).ifPresent(u -> {
                    throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
                });

                user.setName(request.getNickname());
            } else {
                // [LOG] 닉네임 변경 건너뛰기 로깅
                System.out.println("LOG: 닉네임이 현재 값과 동일하여 변경을 건너뜁니다. (닉네임: " + user.getName() + ")");
            }
        }

        // 2. 비밀번호 업데이트
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            if (request.getPassword().length() < 6) {
                throw new IllegalArgumentException("비밀번호는 6자리 이상이어야 합니다.");
            }
            String encodedPassword = passwordEncoder.encode(request.getPassword());
            user.setPassword(encodedPassword);
            System.out.println("LOG: 비밀번호 업데이트 실행됨.");
        }

        // 3. 프로필 이미지 처리
        try {
            if (request.getProfileImage() != null) {
                if (request.getProfileImage().startsWith("data:image")) {
                    // Base64 이미지인 경우 파일로 저장
                    String savedImagePath = saveBase64Image(request.getProfileImage());
                    user.setProfileImage(savedImagePath);
                    System.out.println("LOG: Base64 이미지 저장 완료: " + savedImagePath);
                } else {
                    // 이미 URL이거나 경로인 경우 (예: 기존 경로를 그대로 유지하거나 새로운 경로를 입력한 경우)
                    user.setProfileImage(request.getProfileImage());
                    System.out.println("LOG: 이미지 경로 업데이트: " + request.getProfileImage());
                }
            } else {
                // null인 경우 이미지 삭제 (클라이언트에서 '사진 삭제'를 누른 경우)
                user.setProfileImage(null);
                System.out.println("LOG: 프로필 이미지 삭제됨 (null로 설정).");
            }
        } catch (IllegalArgumentException e) {
            // Base64 형식 오류는 bad request로 전달되도록 다시 던집니다.
            throw new IllegalArgumentException("이미지 파일 형식이 올바르지 않습니다.");
        } catch (Exception e) {
            System.err.println("FATAL ERROR: 이미지 저장 실패: " + e.getMessage());
            // 이미지 저장 오류는 500으로 처리
            throw new RuntimeException("이미지 저장 중 서버 오류가 발생했습니다: " + e.getMessage());
        }

        // 4. 저장 및 업데이트된 엔티티 반환
        try {
            User updatedUser = userRepository.save(user);
            // 💡 추가: 트랜잭션 종료 전 즉시 DB에 변경 사항을 반영하도록 강제
            userRepository.flush();

            System.out.println("LOG: DB 저장 성공. 최종 닉네임: " + updatedUser.getName());
            return updatedUser;
        } catch (Exception e) {
            System.err.println("FATAL ERROR: UserRepository.save() 저장 실패. 원인: " + e.getMessage());
            throw new RuntimeException("DB 저장 중 심각한 오류가 발생했습니다.", e);
        }
    }

}