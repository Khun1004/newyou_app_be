package com.newyou.service;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.newyou.dto.ProfileUpdateRequest;
import com.newyou.entity.User;
import com.newyou.repository.UserRepository;
import com.newyou.sms.SmsSendException;
import com.newyou.sms.SmsSender;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // application.properties에서 설정: file.upload-dir=C:/newyou_uploads/profiles
    @Value("${file.upload-dir:./uploads/profiles}")
    private String uploadDir;

    // 문자 발송기 (sms.provider 설정에 따라 콘솔 / 솔라피 중 하나가 주입됨)
    private final SmsSender smsSender;

    private static final long CODE_TTL_MS = 5 * 60 * 1000; // 인증번호 유효시간 5분
    private static final long RESEND_COOLDOWN_MS = 60 * 1000; // 재발송 대기 60초
    private static final long VERIFIED_TTL_MS = 30 * 60 * 1000; // 인증 완료 후 가입 가능 시간 30분
    private static final int MAX_ATTEMPTS = 5; // 인증번호 입력 최대 시도 횟수

    // 인메모리 저장소 (서버 1대 기준. 서버를 여러 대로 늘리면 Redis/DB로 옮겨야 함)
    private final Map<String, VerificationInfo> verificationCodes = new ConcurrentHashMap<>();
    private final Map<String, Long> verifiedPhones = new ConcurrentHashMap<>(); // 번호 → 만료시각
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, SmsSender smsSender) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.smsSender = smsSender;
    }

    private static class VerificationInfo {
        final String code;
        final long createdAt;
        final long expiryTime;
        int attempts = 0;

        VerificationInfo(String code) {
            this.code = code;
            this.createdAt = System.currentTimeMillis();
            this.expiryTime = createdAt + CODE_TTL_MS;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    /** 숫자만 남기고, 한국 휴대폰 번호 형식(01X로 시작하는 10~11자리)인지 확인합니다. */
    private String normalizePhone(String phoneNumber) {
        String digits = phoneNumber == null ? "" : phoneNumber.replaceAll("[^0-9]", "");
        if (!digits.matches("01[016789]\\d{7,8}")) {
            throw new IllegalArgumentException("올바른 휴대폰 번호를 입력해 주세요.");
        }
        return digits;
    }

    // =======================================================================
    // 1. 휴대폰 인증 로직 (AuthController 사용)
    // =======================================================================

    /** 실제 문자가 발송되는 모드인지 (false = 콘솔 개발 모드) */
    public boolean isRealSmsMode() {
        return smsSender.deliversRealSms();
    }

    /**
     * 6자리 인증번호를 만들어 저장하고, 문자로 발송합니다.
     *
     * @return 생성된 인증번호 (콘솔 개발 모드에서 앱 화면에 보여주기 위해 사용)
     * @throws IllegalArgumentException 번호 형식 오류 / 재발송 대기 중
     * @throws SmsSendException         문자 발송 실패
     */
    public String sendVerificationCode(String phoneNumber) {
        String phone = normalizePhone(phoneNumber);

        VerificationInfo previous = verificationCodes.get(phone);
        if (previous != null) {
            long waitMs = previous.createdAt + RESEND_COOLDOWN_MS - System.currentTimeMillis();
            if (waitMs > 0) {
                throw new IllegalArgumentException(
                        "인증번호를 이미 보냈습니다. " + ((waitMs / 1000) + 1) + "초 후에 다시 요청해 주세요.");
            }
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));

        // 먼저 문자를 보내고, 성공했을 때만 저장합니다. (실패하면 바로 다시 요청할 수 있도록)
        smsSender.send(phone, "[NewYou] 인증번호 [" + code + "]를 입력해 주세요. (5분 내 유효)");

        verificationCodes.put(phone, new VerificationInfo(code));
        verifiedPhones.remove(phone);
        return code;
    }

    /**
     * 휴대폰 인증 코드를 검증합니다. 성공하면 30분 동안 회원가입이 가능해집니다.
     *
     * @throws RuntimeException 인증번호가 없거나 만료되었거나 시도 횟수를 초과한 경우
     */
    public boolean verifyCode(String phoneNumber, String code) {
        String phone = normalizePhone(phoneNumber);
        VerificationInfo info = verificationCodes.get(phone);

        if (info == null) {
            throw new RuntimeException("인증번호를 먼저 요청해 주세요.");
        }

        if (info.isExpired()) {
            verificationCodes.remove(phone);
            throw new RuntimeException("인증 시간이 만료되었습니다. 다시 요청해 주세요.");
        }

        if (info.code.equals(code)) {
            verificationCodes.remove(phone);
            verifiedPhones.put(phone, System.currentTimeMillis() + VERIFIED_TTL_MS);
            return true;
        }

        info.attempts++;
        if (info.attempts >= MAX_ATTEMPTS) {
            verificationCodes.remove(phone);
            throw new RuntimeException("인증번호를 " + MAX_ATTEMPTS + "회 틀렸습니다. 인증번호를 다시 요청해 주세요.");
        }

        return false;
    }

    /** 이 번호가 최근 30분 안에 휴대폰 인증을 마쳤는지 확인합니다. */
    public boolean isPhoneVerified(String phoneNumber) {
        String phone = normalizePhone(phoneNumber);
        Long expiry = verifiedPhones.get(phone);
        if (expiry == null)
            return false;
        if (System.currentTimeMillis() > expiry) {
            verifiedPhones.remove(phone);
            return false;
        }
        return true;
    }

    /** 가입이 끝난 번호의 인증 기록을 지웁니다. (같은 인증으로 두 번 가입 방지) */
    public void consumePhoneVerification(String phoneNumber) {
        verifiedPhones.remove(normalizePhone(phoneNumber));
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