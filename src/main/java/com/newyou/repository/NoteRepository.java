package com.newyou.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.newyou.entity.Note;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    /**
     * 특정 사용자의 모든 노트를 최신 생성일 기준으로 내림차순 조회합니다.
     */
    List<Note> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 특정 사용자의 특정 노트를 ID로 조회합니다. (보안을 위해 userId를 포함)
     */
    Optional<Note> findByIdAndUserId(Long id, Long userId);
}