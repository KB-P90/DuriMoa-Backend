package com.project.domain.calendar.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.calendar.domain.CalendarType;
import com.project.domain.calendar.exception.BadRequestException;
import com.project.domain.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
@Log4j2
public class CalendarController {
    private final CalendarService service;

    @GetMapping()
    public ResponseEntity<ApiResponse<?>> getMonthlyRecord(
            @RequestParam String month,
            @RequestParam String type
            ) {
        // TODO 로그인 구현 후 Security에서 가져오기
        Long userId = 1L;

        YearMonth yearMonth;
        CalendarType calendarType;

        try {
            yearMonth = YearMonth.parse(month);
            calendarType = CalendarType.valueOf(type.toUpperCase());
        } catch (DateTimeParseException  e) {
            throw new BadRequestException("month는 yyyy-MM 형식이어야 합니다.");
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("type은 personal과 wedding 중 하나여야 합니다.");
        }

        return ResponseEntity.ok(ApiResponse.success(service.getMonthly(userId, yearMonth, calendarType)));
    }
}
