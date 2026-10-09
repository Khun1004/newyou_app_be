package com.newyou.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.newyou.dto.MoneyRequest;
import com.newyou.dto.MoneyResponse;
import com.newyou.entity.MoneyRecord;
import com.newyou.entity.User;
import com.newyou.repository.MoneyRecordRepository;

@Service
public class MoneyService {

    private final MoneyRecordRepository repository;

    public MoneyService(MoneyRecordRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<MoneyResponse> getMyRecords(Long userId, String month) {
        List<MoneyRecord> list = (month == null || month.isBlank())
                ? repository.findByUserIdOrderByDateDescIdDesc(userId)
                : repository.findByUserIdAndDateStartingWithOrderByDateDescIdDesc(userId, month);
        return list.stream().map(MoneyResponse::new).toList();
    }

    @Transactional
    public MoneyResponse create(User user, MoneyRequest req) {
        validate(req, true);
        MoneyRecord r = new MoneyRecord();
        r.setUser(user);
        apply(r, req);
        return new MoneyResponse(repository.save(r));
    }

    @Transactional
    public MoneyResponse update(Long id, Long userId, MoneyRequest req) {
        MoneyRecord r = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 접근 권한이 없는 기록입니다."));
        validate(req, false);
        apply(r, req);
        return new MoneyResponse(r);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        MoneyRecord r = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 접근 권한이 없는 기록입니다."));
        repository.delete(r);
    }

    private void apply(MoneyRecord r, MoneyRequest req) {
        if (req.getType() != null) r.setType(req.getType());
        if (req.getAmount() != null) r.setAmount(req.getAmount());
        if (req.getCategory() != null) r.setCategory(req.getCategory().trim());
        if (req.getMemo() != null) r.setMemo(req.getMemo().trim());
        if (req.getDate() != null) r.setDate(req.getDate());
    }

    private void validate(MoneyRequest req, boolean creating) {
        if (creating || req.getType() != null) {
            if (!"INCOME".equals(req.getType()) && !"EXPENSE".equals(req.getType())) {
                throw new IllegalArgumentException("수입/지출 구분이 올바르지 않습니다.");
            }
        }
        if (creating || req.getAmount() != null) {
            if (req.getAmount() == null || req.getAmount() <= 0) {
                throw new IllegalArgumentException("금액을 입력해 주세요.");
            }
        }
        if (creating || req.getCategory() != null) {
            if (req.getCategory() == null || req.getCategory().isBlank()) {
                throw new IllegalArgumentException("분류를 골라 주세요.");
            }
        }
        if (creating || req.getDate() != null) {
            if (req.getDate() == null || !req.getDate().matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw new IllegalArgumentException("날짜 형식이 올바르지 않습니다. (예: 2026-10-10)");
            }
        }
        if (req.getMemo() != null && req.getMemo().length() > 200) {
            throw new IllegalArgumentException("메모는 200자까지 쓸 수 있어요.");
        }
    }
}