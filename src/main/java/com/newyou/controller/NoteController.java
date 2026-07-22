package com.newyou.controller;

import java.util.List;

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
    public ResponseEntity<Note> createNote(
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
            return ResponseEntity.status(HttpStatus.CREATED).body(createdNote);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 내 모든 노트 조회
     * GET /api/notes
     */
    @GetMapping
    public ResponseEntity<List<Note>> getMyNotes(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<Note> notes = noteService.getNotesByUserId(user.getId());
        return ResponseEntity.ok(notes);
    }

    /**
     * 노트 상세 조회
     * GET /api/notes/{noteId}
     */
    @GetMapping("/{noteId}")
    public ResponseEntity<Note> getNote(
            @AuthenticationPrincipal User user,
            @PathVariable Long noteId) {

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Note note = noteService.getNoteById(noteId, user.getId());
            return ResponseEntity.ok(note);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * 노트 수정
     * PUT /api/notes/{noteId}
     */
    @PutMapping("/{noteId}")
    public ResponseEntity<Note> updateNote(
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
            return ResponseEntity.ok(updatedNote);
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
}