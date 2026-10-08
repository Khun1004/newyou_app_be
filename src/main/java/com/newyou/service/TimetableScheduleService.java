package com.newyou.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.newyou.dto.ScheduleRequest;
import com.newyou.dto.ScheduleResponse;
import com.newyou.entity.TimetableSchedule;
import com.newyou.entity.User;
import com.newyou.repository.TimetableScheduleRepository;

@Service
public class TimetableScheduleService {

    private static final Set<String> DAYS = Set.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun");

    private final TimetableScheduleRepository repository;

    public TimetableScheduleService(TimetableScheduleRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> getMySchedules(Long userId) {
        return repository.findByUserIdOrderByDayAscTimeAsc(userId).stream()
                .map(ScheduleResponse::new)
                .toList();
    }

    @Transactional
    public ScheduleResponse create(User user, ScheduleRequest req) {
        validate(req, true);
        TimetableSchedule s = new TimetableSchedule();
        s.setUser(user);
        apply(s, req);
        return new ScheduleResponse(repository.save(s));
    }

    @Transactional
    public ScheduleResponse update(Long id, Long userId, ScheduleRequest req) {
        TimetableSchedule s = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 접근 권한이 없는 일정입니다."));
        validate(req, false);
        apply(s, req);
        return new ScheduleResponse(s);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        TimetableSchedule s = repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않거나 접근 권한이 없는 일정입니다."));
        repository.delete(s);
    }

    // 값이 들어온 것만 바꿔요.
    private void apply(TimetableSchedule s, ScheduleRequest req) {
        if (req.getTitle() != null)
            s.setTitle(req.getTitle().trim());
        if (req.getDay() != null)
            s.setDay(req.getDay());
        if (req.getTime() != null)
            s.setTime(req.getTime());
        if (req.getDuration() != null)
            s.setDuration(req.getDuration());
        if (req.getColor() != null)
            s.setColor(req.getColor());
    }

    private void validate(ScheduleRequest req, boolean creating) {
        if (creating || req.getTitle() != null) {
            if (req.getTitle() == null || req.getTitle().isBlank()) {
                throw new IllegalArgumentException("일정 제목을 입력해 주세요.");
            }
        }
        if (creating || req.getDay() != null) {
            if (req.getDay() == null || !DAYS.contains(req.getDay())) {
                throw new IllegalArgumentException("요일이 올바르지 않습니다.");
            }
        }
        if (creating || req.getTime() != null) {
            if (req.getTime() == null || !req.getTime().matches("([01]\\d|2[0-3]):[0-5]\\d")) {
                throw new IllegalArgumentException("시간 형식이 올바르지 않습니다. (예: 09:30)");
            }
        }
        if (creating || req.getDuration() != null) {
            if (req.getDuration() == null || req.getDuration() <= 0 || req.getDuration() > 24) {
                throw new IllegalArgumentException("시간 길이는 0보다 크고 24시간 이하여야 합니다.");
            }
        }
    }
}