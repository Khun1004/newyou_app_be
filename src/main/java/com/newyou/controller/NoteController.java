package com.newyou.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.newyou.dto.NoteResponse;
import com.newyou.entity.Note;
import com.newyou.entity.User;
import com.newyou.service.NoteService;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;

    // 👇 design 필드 추가
    public static class NoteRequest {
        public String title;
        public String content;
        public String category;
        public String design; // 👈 추가
    }

    @Autowired
    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    /**
     * 새 노트 작성
     * POST /api/notes
     */
    @PostMapping
    public ResponseEntity<NoteResponse> createNote(
            @AuthenticationPrincipal User user,
            @RequestBody NoteRequest request) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Note createdNote = noteService.createNote(
                    user.getId(),
                    request.title,
                    request.content,
                    request.category,
                    request.design); // 👈 design 추가
            return ResponseEntity.status(HttpStatus.CREATED).body(NoteResponse.full(createdNote));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 내 모든 노트 조회
     * GET /api/notes
     */
    @GetMapping
    public ResponseEntity<List<NoteResponse>> getMyNotes(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 잠긴 메모는 내용을 숨기고 보내요
        List<NoteResponse> notes = noteService.getNotesByUserId(user.getId())
                .stream().map(NoteResponse::masked).toList();
        return ResponseEntity.ok(notes);
    }

    /**
     * 노트 상세 조회
     * GET /api/notes/{noteId}
     */
    @GetMapping("/{noteId}")
    public ResponseEntity<NoteResponse> getNote(
            @AuthenticationPrincipal User user,
            @PathVariable Long noteId) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Note note = noteService.getNoteById(noteId, user.getId());
            return ResponseEntity.ok(NoteResponse.masked(note));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * 노트 수정
     * PUT /api/notes/{noteId}
     */
    @PutMapping("/{noteId}")
    public ResponseEntity<NoteResponse> updateNote(
            @AuthenticationPrincipal User user,
            @PathVariable Long noteId,
            @RequestBody NoteRequest request) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Note updatedNote = noteService.updateNote(
                    noteId,
                    user.getId(),
                    request.title,
                    request.content,
                    request.category,
                    request.design); // 👈 design 추가
            return ResponseEntity.ok(NoteResponse.full(updatedNote));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * 노트 삭제
     * DELETE /api/notes/{noteId}
     */
    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> deleteNote(
            @AuthenticationPrincipal User user,
            @PathVariable Long noteId) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            noteService.deleteNote(noteId, user.getId());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // =======================================================================
    // 🔒 메모 잠금
    // =======================================================================

    /** GET /api/notes/pin → { hasPin: true/false } */
    @GetMapping("/pin")
    public ResponseEntity<?> pinStatus(@AuthenticationPrincipal User user) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(Map.of("hasPin", noteService.hasPin(user.getId())));
    }

    /** POST /api/notes/pin { pin } → 처음 PIN 만들기 */
    @PostMapping("/pin")
    public ResponseEntity<?> createPin(@AuthenticationPrincipal User user, @RequestBody Map<String, String> body) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            noteService.createPin(user.getId(), body.get("pin"));
            return ResponseEntity.ok(Map.of("message", "메모 PIN을 만들었어요."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** POST /api/notes/pin/reset { password, pin } → PIN을 잊었을 때 계정 비밀번호로 새로 만들기 */
    @PostMapping("/pin/reset")
    public ResponseEntity<?> resetPin(@AuthenticationPrincipal User user, @RequestBody Map<String, String> body) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            noteService.resetPin(user.getId(), body.get("password"), body.get("pin"));
            return ResponseEntity.ok(Map.of("message", "새 메모 PIN으로 바꿨어요."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** POST /api/notes/{noteId}/unlock { pin } → PIN이 맞으면 내용까지 돌려줘요 */
    @PostMapping("/{noteId}/unlock")
    public ResponseEntity<?> unlockNote(@AuthenticationPrincipal User user, @PathVariable Long noteId,
                                        @RequestBody Map<String, String> body) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            return ResponseEntity.ok(NoteResponse.full(noteService.unlockNote(noteId, user.getId(), body.get("pin"))));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** PUT /api/notes/{noteId}/lock { locked: true/false, pin } → 잠그기 / 잠금 풀기 */
    @PutMapping("/{noteId}/lock")
    public ResponseEntity<?> setLocked(@AuthenticationPrincipal User user, @PathVariable Long noteId,
                                       @RequestBody Map<String, Object> body) {
        if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        boolean locked = Boolean.TRUE.equals(body.get("locked"));
        String pin = body.get("pin") == null ? null : String.valueOf(body.get("pin"));
        try {
            return ResponseEntity.ok(NoteResponse.full(noteService.setLocked(noteId, user.getId(), locked, pin)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}