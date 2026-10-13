package com.newyou.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.newyou.entity.Note;
import com.newyou.entity.User;
import com.newyou.repository.NoteRepository;
import com.newyou.repository.UserRepository;

@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public NoteService(NoteRepository noteRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.noteRepository = noteRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 1. 노트 생성 - 👇 design 파라미터 추가
    @Transactional
    public Note createNote(Long userId, String title, String content, String category, String design) {
        if (title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("제목과 내용을 모두 입력해야 합니다.");
        }

        Note newNote = new Note(userId, title, content, category, design);

        return noteRepository.save(newNote);
    }

    // 2. 노트 조회 (특정 사용자)
    public List<Note> getNotesByUserId(Long userId) {
        return noteRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // 3. 노트 상세 조회 (사용자 소유 확인)
    public Note getNoteById(Long noteId, Long userId) {
        return noteRepository.findByIdAndUserId(noteId, userId)
                .orElseThrow(() -> new IllegalArgumentException("노트를 찾을 수 없거나 접근 권한이 없습니다."));
    }

    // 4. 노트 수정 - 👇 design 파라미터 추가
    @Transactional
    public Note updateNote(Long noteId, Long userId, String newTitle, String newContent, String newCategory,
                           String newDesign) {
        Note note = getNoteById(noteId, userId);

        if (newTitle == null || newTitle.trim().isEmpty() || newContent == null || newContent.trim().isEmpty()) {
            throw new IllegalArgumentException("수정할 제목과 내용을 모두 입력해야 합니다.");
        }

        note.setTitle(newTitle);
        note.setContent(newContent);
        note.setCategory(newCategory);
        note.setDesign(newDesign); // 👈 design 설정 추가
        note.setUpdatedAt(LocalDateTime.now());

        return noteRepository.save(note);
    }

    // 5. 노트 삭제
    @Transactional
    public void deleteNote(Long noteId, Long userId) {
        Note note = getNoteById(noteId, userId);
        noteRepository.delete(note);
    }

    // =======================================================================
    // 🔒 메모 잠금 (PIN 4자리)
    // =======================================================================

    /** PIN을 5번 틀리면 5분 동안 막아요 (userId → [틀린 횟수, 풀리는 시각]) */
    private final Map<Long, long[]> pinFails = new ConcurrentHashMap<>();
    private static final int MAX_FAILS = 5;
    private static final long BLOCK_MILLIS = 5 * 60 * 1000L;

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없어요."));
    }

    private static void checkPinFormat(String pin) {
        if (pin == null || !pin.matches("\\d{4}")) {
            throw new IllegalArgumentException("PIN은 숫자 4자리로 만들어 주세요.");
        }
    }

    public boolean hasPin(Long userId) {
        String pin = loadUser(userId).getNotePin();
        return pin != null && !pin.isBlank();
    }

    /** 처음 PIN 만들기 (이미 있으면 안 돼요 → 잊었으면 resetPin) */
    @Transactional
    public void createPin(Long userId, String pin) {
        checkPinFormat(pin);
        User user = loadUser(userId);
        if (user.getNotePin() != null && !user.getNotePin().isBlank()) {
            throw new IllegalStateException("이미 메모 PIN이 있어요.");
        }
        user.setNotePin(passwordEncoder.encode(pin));
        userRepository.save(user);
    }

    /** PIN을 잊었을 때: 계정 비밀번호를 확인하고 새 PIN으로 바꿔요 */
    @Transactional
    public void resetPin(Long userId, String accountPassword, String newPin) {
        checkPinFormat(newPin);
        User user = loadUser(userId);
        if (accountPassword == null || !passwordEncoder.matches(accountPassword, user.getPassword())) {
            throw new IllegalArgumentException("계정 비밀번호가 맞지 않아요.");
        }
        user.setNotePin(passwordEncoder.encode(newPin));
        userRepository.save(user);
        pinFails.remove(userId);
    }

    /** PIN 확인 (틀리면 IllegalArgumentException) */
    public void verifyPin(Long userId, String pin) {
        long now = System.currentTimeMillis();
        long[] fail = pinFails.get(userId);
        if (fail != null && fail[0] >= MAX_FAILS && now < fail[1]) {
            long min = Math.max(1, (fail[1] - now) / 60000 + 1);
            throw new IllegalArgumentException("PIN을 여러 번 틀렸어요. " + min + "분 뒤에 다시 해 주세요.");
        }
        User user = loadUser(userId);
        if (user.getNotePin() == null || user.getNotePin().isBlank()) {
            throw new IllegalArgumentException("메모 PIN을 먼저 만들어 주세요.");
        }
        if (pin == null || !passwordEncoder.matches(pin, user.getNotePin())) {
            // 마지막으로 틀린 지 5분이 지났으면 다시 1번부터 세요
            long count = (fail == null || now >= fail[1]) ? 1 : fail[0] + 1;
            pinFails.put(userId, new long[] { count, now + BLOCK_MILLIS });
            long left = MAX_FAILS - count;
            throw new IllegalArgumentException(left > 0
                    ? "PIN이 맞지 않아요. (" + left + "번 남았어요)"
                    : "PIN을 여러 번 틀렸어요. 5분 뒤에 다시 해 주세요.");
        }
        pinFails.remove(userId);
    }

    /** 잠긴 메모 열기: PIN이 맞으면 내용까지 돌려줘요 */
    public Note unlockNote(Long noteId, Long userId, String pin) {
        Note note = getNoteById(noteId, userId);
        if (note.isLocked()) {
            verifyPin(userId, pin);
        }
        return note;
    }

    /** 잠그기 / 잠금 풀기 (잠금 풀기는 PIN이 필요해요) */
    @Transactional
    public Note setLocked(Long noteId, Long userId, boolean locked, String pin) {
        Note note = getNoteById(noteId, userId);
        if (locked) {
            if (!hasPin(userId)) {
                throw new IllegalArgumentException("메모 PIN을 먼저 만들어 주세요.");
            }
        } else if (note.isLocked()) {
            verifyPin(userId, pin);
        }
        note.setLocked(locked);
        return noteRepository.save(note);
    }
}