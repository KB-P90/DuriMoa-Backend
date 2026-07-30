package com.project.domain.calendar.controller;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.common.response.ApiResponse;
import com.project.domain.calendar.domain.CalendarType;
import com.project.domain.calendar.dto.request.RecordRequestDTO;
import com.project.domain.calendar.dto.response.RecordResponseDTO;
import com.project.domain.calendar.exception.BadRequestException;
import com.project.domain.calendar.service.CalendarService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
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
@Api(tags = "Calendar Record", description = "거래 내역 관리 API")
public class CalendarController {
    private final CalendarService service;

    @ApiOperation(
            value = "월별 거래 내역 조회",
            notes = "사용자의 월별 거래 내역을 조회합니다. month는 yyyy-MM 형식이며, type은 personal 또는 wedding으로 조회 범위를 지정합니다."
    )
    @GetMapping("/monthly")
    public ResponseEntity<ApiResponse<?>> getMonthlyRecord(
//            @AuthenticationPrincipal Long userId,
            Authentication authentication,
            @ApiParam(value = "조회 월 (yyyy-MM 형식)", example = "2026-07")
            @RequestParam String month,
            @ApiParam(value = "조회 타입 (personal 또는 wedding)", example = "personal")
            @RequestParam String type
    ) {
        Long userId = (Long) authentication.getPrincipal();

        YearMonth yearMonth;
        CalendarType calendarType;

        try {
            yearMonth = YearMonth.parse(month);
            calendarType = CalendarType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return ResponseEntity.ok(ApiResponse.success(service.getMonthly(userId, yearMonth, calendarType)));
    }

    @ApiOperation(
            value = "일별 거래 내역 조회",
            notes = "사용자의 특정 날짜 거래 내역을 조회합니다. date는 yyyy-MM-dd 형식이며, type은 personal 또는 wedding으로 조회 범위를 지정합니다."
    )
    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<?>> getDailyRecord(
//            @AuthenticationPrincipal Long userId,
            Authentication authentication,
            @ApiParam(value = "조회 날짜 (yyyy-MM-dd 형식)", example = "2026-07-30")
            @RequestParam String date,
            @ApiParam(value = "조회 타입 (personal 또는 wedding)", example = "personal")
            @RequestParam String type
    ) {
        Long userId = (Long) authentication.getPrincipal();

        LocalDate localDate;
        CalendarType calendarType;

        try {
            localDate = LocalDate.parse(date);
            calendarType = CalendarType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return ResponseEntity.ok(ApiResponse.success(service.getDaily(userId, localDate, calendarType)));
    }

    @ApiOperation(
            value = "거래 내역 등록",
            notes = "사용자의 수입, 지출, 저축 거래 내역을 등록합니다."
    )
    @PostMapping()
    public ResponseEntity<ApiResponse<RecordResponseDTO>> create(
//            @AuthenticationPrincipal Long userId,
            Authentication authentication,
            @RequestBody RecordRequestDTO record
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return ResponseEntity.ok(ApiResponse.success("내역이 등록되었습니다.", service.create(userId, record)));
    }

    @ApiOperation(
            value = "거래 내역 수정",
            notes = "등록된 거래 내역의 유형, 카테고리, 금액, 날짜, 메모 정보를 수정합니다."
    )
    @PatchMapping("/{recordId}")
    public ResponseEntity<ApiResponse<RecordResponseDTO>> update(
//            @AuthenticationPrincipal Long userId,
            Authentication authentication,
            @ApiParam(value = "거래 내역 ID", example = "1")
            @PathVariable Long recordId,
            @RequestBody RecordRequestDTO record
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return ResponseEntity.ok(ApiResponse.success("내역이 수정되었습니다.", service.update(userId, recordId, record)));
    }

    @ApiOperation(
            value = "거래 내역 삭제",
            notes = "등록된 거래 내역을 삭제합니다."
    )
    @DeleteMapping("/{recordId}")
    public ResponseEntity<ApiResponse<RecordResponseDTO>> delete(
//            @AuthenticationPrincipal Long userId,
            Authentication authentication,
            @ApiParam(value = "거래 내역 ID", example = "1")
            @PathVariable Long recordId
    ) {
        Long userId = (Long) authentication.getPrincipal();

        return ResponseEntity.ok(ApiResponse.success("내역이 삭제되었습니다.", service.delete(userId, recordId)));
    }
}
