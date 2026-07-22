package com.newyou.service;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.newyou.dto.AlarmDto;
import com.newyou.entity.Alarm;
import com.newyou.entity.User;
import com.newyou.repository.AlarmRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService {

    private final AlarmRepository alarmRepository;

    // application.properties에서 설정: file.upload-dir.voice=C:/newyou_uploads/voices
    @Value("${file.upload-dir.voice:./uploads/voices}")
    private String voiceUploadDir; // 음성 파일 저장 경로

    /**
     * 사용자의 모든 알람 조회
     */
    @Transactional(readOnly = true)
    public List<AlarmDto> getAlarmsByUserId(Long userId) {
        log.info("사용자 ID: {}의 알람 목록 조회 시작", userId);
        List<Alarm> alarms = alarmRepository.findByUserId(userId);

        return alarms.stream()
                .map(AlarmDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 새로운 알람 추가 (음성 파일 처리 포함)
     */
    @Transactional
    public AlarmDto createAlarm(User user, AlarmDto alarmDto) {
        // 클라이언트 ID(UUID)가 없다면 새로 생성합니다.
        String alarmId = alarmDto.getId() != null && !alarmDto.getId().isEmpty()
                ? alarmDto.getId()
                : UUID.randomUUID().toString();

        // 💡 FIX 1: sound 값이 null일 경우 "default"로 대체합니다. (NOT NULL 제약 조건 해결)
        String soundValue = alarmDto.getSound() != null ? alarmDto.getSound() : "default";

        // 💡 FIX 2: 시간 필드 포맷팅 적용 (Data truncation: time 오류 해결)
        String rawTime = alarmDto.getTime();
        String formattedTime = formatAlarmTime(rawTime);
        log.info("⏰ TIME TRACE: Raw Time: \"{}\" -> Formatted Time: \"{}\"", rawTime, formattedTime);

        // 1. 음성 파일 처리
        String savedVoicePath = handleVoiceFile(alarmDto.getVoiceUri(), alarmId, "CREATE");

        // 2. Alarm Entity 생성
        Alarm newAlarm = Alarm.builder()
                .id(alarmId)
                .user(user)
                .time(formattedTime) // 포맷팅된 시간 사용
                .label(alarmDto.getLabel())
                .isActive(alarmDto.isActive())
                .voiceUri(savedVoicePath) // 저장된 경로
                .repeat(alarmDto.getRepeat())
                .sound(soundValue) // 수정된 soundValue 사용
                .build();

        // 3. DB 저장
        Alarm savedAlarm = alarmRepository.save(newAlarm);
        log.info("✅ 새로운 알람 저장 성공. ID: {}, Time: {}", savedAlarm.getId(), savedAlarm.getTime());

        return AlarmDto.fromEntity(savedAlarm);
    }

    /**
     * 알람 정보 수정 (음성 파일 처리 포함)
     */
    @Transactional
    public AlarmDto updateAlarm(Long userId, String alarmId, AlarmDto alarmDto) {
        // 1. 사용자 ID와 알람 ID로 알람 조회 및 소유권 확인
        Alarm alarm = alarmRepository.findByUserIdAndId(userId, alarmId)
                .orElseThrow(() -> new IllegalArgumentException("알람을 찾을 수 없거나 접근 권한이 없습니다."));

        // 2. sound 값이 null일 경우 "default"로 대체 (NOT NULL 제약 조건 방지)
        String soundValue = alarmDto.getSound() != null ? alarmDto.getSound() : "default";

        // 3. 시간 필드 포맷팅 적용 (DB Data truncation 오류 해결)
        String formattedTime = formatAlarmTime(alarmDto.getTime());
        log.info("⏰ TIME TRACE (Update): Raw Time: \"{}\" -> Formatted Time: \"{}\"", alarmDto.getTime(),
                formattedTime);

        // 4. 음성 파일 처리 (기존 파일 삭제/새 파일 저장/유지)
        // savedVoicePath는 새 파일 경로, 기존 경로, 또는 null을 반환합니다.
        String savedVoicePath = handleVoiceFile(alarmDto.getVoiceUri(), alarmId, "UPDATE");

        // 5. 알람 엔티티 업데이트
        alarm.setTime(formattedTime); // 포맷팅된 시간 사용
        alarm.setLabel(alarmDto.getLabel());
        alarm.setActive(alarmDto.isActive());
        alarm.setRepeat(alarmDto.getRepeat());
        alarm.setSound(soundValue);

        // handleVoiceFile의 반환 값을 그대로 사용합니다.
        // Base64 파일을 저장했거나, null (삭제 요청), 기존 경로 유지 중 하나입니다.
        alarm.setVoiceUri(savedVoicePath);

        // 6. DB 저장
        Alarm updatedAlarm = alarmRepository.save(alarm);
        log.info("✅ 알람 업데이트 성공. ID: {}, Time: {}", updatedAlarm.getId(), updatedAlarm.getTime());

        return AlarmDto.fromEntity(updatedAlarm);
    }

    /**
     * 알람 활성화/비활성화 상태 토글
     */
    @Transactional
    public AlarmDto toggleAlarm(Long userId, String alarmId) {
        Alarm alarm = alarmRepository.findByUserIdAndId(userId, alarmId)
                .orElseThrow(() -> new IllegalArgumentException("알람을 찾을 수 없거나 접근 권한이 없습니다."));

        // 💡 현재 isActive 상태를 반전시킵니다.
        alarm.setActive(!alarm.isActive());

        Alarm updatedAlarm = alarmRepository.save(alarm);
        log.info("✅ 알람 상태 토글 성공. ID: {}, isActive: {}", updatedAlarm.getId(), updatedAlarm.isActive());

        return AlarmDto.fromEntity(updatedAlarm);
    }

    /**
     * 알람 삭제
     */
    @Transactional
    public void deleteAlarm(Long userId, String alarmId) {
        // 알람을 조회하여 소유권 확인
        Alarm alarm = alarmRepository.findByUserIdAndId(userId, alarmId)
                .orElseThrow(() -> new IllegalArgumentException("알람을 찾을 수 없거나 접근 권한이 없습니다."));

        // 1. 음성 파일 삭제 (있는 경우)
        if (alarm.getVoiceUri() != null) {
            deleteVoiceFile(alarm.getVoiceUri());
        }

        // 2. DB에서 삭제 (pk 사용)
        alarmRepository.delete(alarm);
        log.info("✅ 알람 삭제 성공. ID: {}", alarmId);
    }

    // =========================================================================
    // 📢 헬퍼 메서드: 시간 포맷팅 (Data truncation 해결)
    // =========================================================================

    /**
     * 알람 시간을 DB의 'time' 컬럼(VARCHAR(5))에 맞게 "HH:mm" 형식으로 포맷합니다.
     */
    private String formatAlarmTime(String rawTime) {
        if (rawTime == null || rawTime.isEmpty()) {
            throw new IllegalArgumentException("Alarm time value is missing.");
        }

        // 길이가 5를 초과하는 경우에만 처리합니다. (예: ISO 8601 문자열)
        if (rawTime.length() > 5) {
            log.warn("⚠️ Alarm time value is too long (length: {}). Attempting to extract 'HH:mm'. Original: {}",
                    rawTime.length(), rawTime);

            // Case 1: ISO 8601 (e.g., "2025-11-27T23:30:00.000Z")
            int tIndex = rawTime.indexOf('T');
            if (tIndex != -1 && rawTime.length() >= tIndex + 6) {
                // 'T' 다음의 5글자(HH:mm)를 추출합니다.
                String timePart = rawTime.substring(tIndex + 1, tIndex + 6);
                // 최종적으로 5글자를 초과하는지 다시 한번 안전 점검
                if (timePart.length() > 5) {
                    timePart = timePart.substring(0, 5);
                }
                return timePart;
            }

            // Case 2: 기타 긴 형식인 경우 앞에서부터 5글자를 자릅니다.
            String truncatedTime = rawTime.substring(0, 5);
            return truncatedTime;
        }

        // 길이가 5 이하인 경우 그대로 사용합니다.
        return rawTime;
    }

    // =========================================================================
    // 📢 음성 파일 처리 로직 (voiceUri 긴 경로 문제 해결)
    // =========================================================================

    private String handleVoiceFile(String voiceUri, String alarmId, String operation) {
        if (voiceUri == null) {
            // 삭제 요청 또는 파일 없음
            if (operation.equals("UPDATE")) {
                // UPDATE 시 voiceUri가 null이면 기존 파일 삭제 요청
                alarmRepository.findById(alarmId).ifPresent(alarm -> {
                    if (alarm.getVoiceUri() != null) {
                        deleteVoiceFile(alarm.getVoiceUri());
                    }
                });
            }
            log.info("LOG: 음성 파일 없음 또는 삭제 요청.");
            return null;
        }

        // Base64 데이터인지 확인 (새 파일 업로드)
        if (voiceUri.startsWith("data:audio")) {
            log.info("LOG: Base64 음성 파일 발견. 저장 시작.");

            // 기존 파일이 있다면 삭제 (UPDATE 시)
            if (operation.equals("UPDATE")) {
                alarmRepository.findById(alarmId).ifPresent(alarm -> {
                    if (alarm.getVoiceUri() != null) {
                        deleteVoiceFile(alarm.getVoiceUri());
                    }
                });
            }

            return saveBase64VoiceFile(voiceUri, alarmId);

            // 💡 FIX 3: 기존 경로 (짧은 서버 상대 경로)인지 명확히 확인합니다.
        } else if (voiceUri.startsWith("/uploads/voices/")) {
            // 기존 서버 경로 유지 (짧은 상대 경로)
            log.info("LOG: 기존 음성 파일 경로 유지: {}", voiceUri);
            return voiceUri;
        } else {
            // 모바일 로컬 경로(`file:///...`)와 같이 알 수 없는 긴 경로는 무시합니다.
            log.warn("⚠️ LOG: 알 수 없는 형식의 Voice URI 수신 (모바일 로컬 경로 추정) - 저장 무시: {}", voiceUri);

            // UPDATE 요청에서 로컬 경로가 넘어왔다면, 기존 DB 값을 유지해야 합니다.
            if (operation.equals("UPDATE")) {
                return alarmRepository.findById(alarmId)
                        .map(Alarm::getVoiceUri)
                        .orElse(null);
            }

            // CREATE 요청에서는 null을 반환하여 voiceUri에 값이 들어가지 않도록 합니다.
            return null;
        }
    }

    private String saveBase64VoiceFile(String base64Data, String alarmId) {
        try {
            // MIME 타입 및 Base64 데이터 추출
            String[] parts = base64Data.split(",");
            if (parts.length < 2) {
                throw new IllegalArgumentException("올바른 Base64 데이터 형식이 아닙니다.");
            }
            String mimeTypePart = parts[0];
            String base64Content = parts[1];

            // 파일 확장자 결정 (간단하게 처리)
            String extension = mimeTypePart.contains("mpeg") ? ".mp3" : ".m4a";
            if (mimeTypePart.contains("ogg"))
                extension = ".ogg";
            else if (mimeTypePart.contains("wav"))
                extension = ".wav"; // WAV 파일 추가

            byte[] decodedBytes = Base64.getDecoder().decode(base64Content);

            // 파일 이름 및 경로 설정
            String fileName = "voice_" + alarmId + extension;
            Path uploadPath = Paths.get(voiceUploadDir).toAbsolutePath().normalize();
            Path targetPath = uploadPath.resolve(fileName);

            // 디렉토리가 없으면 생성
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("LOG: 음성 파일 저장 디렉토리 생성: {}", uploadPath.toString());
            }

            // 파일 저장
            try (FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
                fos.write(decodedBytes);
            }

            // DB에 저장할 상대 경로 반환 (WebConfig에서 설정한 경로 사용)
            String relativePath = "/uploads/voices/" + fileName;
            log.info("LOG: 음성 파일 저장 성공: {}", relativePath);
            return relativePath;

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("음성 파일 형식이 올바르지 않습니다.");
        } catch (Exception e) {
            log.error("FATAL ERROR: 음성 파일 저장 실패", e);
            throw new RuntimeException("음성 파일 저장 중 서버 오류가 발생했습니다.");
        }
    }

    private void deleteVoiceFile(String relativePath) {
        try {
            // 상대 경로에서 파일 이름 추출
            String fileName = Paths.get(relativePath).getFileName().toString();
            Path uploadPath = Paths.get(voiceUploadDir).toAbsolutePath().normalize();
            Path targetPath = uploadPath.resolve(fileName);

            if (Files.exists(targetPath)) {
                Files.delete(targetPath);
                log.warn("⚠️ 기존 음성 파일 삭제 완료: {}", targetPath.toString());
            } else {
                log.warn("⚠️ 기존 음성 파일이 존재하지 않아 삭제를 건너뜁니다: {}", targetPath.toString());
            }

        } catch (Exception e) {
            log.error("❌ 음성 파일 삭제 실패: {}", relativePath, e);
            // 파일 시스템 오류가 DB 트랜잭션을 막지는 않도록 경고 처리만 합니다.
        }
    }
}