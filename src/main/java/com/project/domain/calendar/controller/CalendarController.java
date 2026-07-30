package com.project.domain.calendar.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.calendar.domain.CalendarType;
import com.project.domain.calendar.dto.request.RecordRequestDTO;
import com.project.domain.calendar.dto.response.RecordResponseDTO;
import com.project.domain.calendar.exception.BadRequestException;
import com.project.domain.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
@Log4j2
public class CalendarController {
    private final CalendarService service;

    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<?>> getMonthlyRecord(
//            @AuthenticationPrincipal Long userId,
            Authentication authentication,
            @RequestParam String month,
            @RequestParam String type
    ) {
        Long userId = (Long) authentication.getPrincipal();

        YearMonth yearMonth;
        CalendarType calendarType;

        try {
            yearMonth = YearMonth.parse(month);
            calendarType = CalendarType.valueOf(type.toUpperCase());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("month는 yyyy-MM 형식이어야 합니다.");
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("type은 personal과 wedding 중 하나여야 합니다.");
        }

        return ResponseEntity.ok(ApiResponse.success(service.getMonthly(userId, yearMonth, calendarType)));
    }

    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<?>> getDailyRecord(
//            @AuthenticationPrincipal Long userId,
            Authentication authentication,
            @RequestParam String date,
            @RequestParam String type
    ) {
        Long userId = (Long) authentication.getPrincipal();

        LocalDate localDate;
        CalendarType calendarType;

        try {
            localDate = LocalDate.parse(date);
            calendarType = CalendarType.valueOf(type.toUpperCase());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("date는 yyyy-MM-dd 형식이어야 합니다.");
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("type은 personal과 wedding 중 하나여야 합니다.");
        }

        return ResponseEntity.ok(ApiResponse.success(service.getDaily(userId, localDate, calendarType)));
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<RecordResponseDTO>> create(
//            @AuthenticationPrincipal Long userId,
            Authentication authentication,
            @RequestBody RecordRequestDTO record
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return ResponseEntity.ok(ApiResponse.success("내역이 등록되었습니다.", service.create(userId, record)));
    }

    @PatchMapping("/{recordId}")
    public ResponseEntity<ApiResponse<RecordResponseDTO>> update(
//            @AuthenticationPrincipal Long userId,
            Authentication authentication,
            @PathVariable Long recordId,
            @RequestBody RecordRequestDTO record
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return ResponseEntity.ok(ApiResponse.success("내역이 수정되었습니다.", service.update(userId, recordId, record)));
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<ApiResponse<RecordResponseDTO>> delete(
//            @AuthenticationPrincipal Long userId,
            Authentication authentication,
            @PathVariable Long recordId
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return ResponseEntity.ok(ApiResponse.success("내역이 삭제되었습니다.", service.delete(userId, recordId)));
    }
}
