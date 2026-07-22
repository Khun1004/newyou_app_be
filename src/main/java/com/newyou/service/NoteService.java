package com.newyou.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.newyou.entity.Note;
import com.newyou.repository.NoteRepository;

@Service
public class NoteService {

    private final NoteRepository noteRepository;

    @Autowired
    public NoteService(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
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
}