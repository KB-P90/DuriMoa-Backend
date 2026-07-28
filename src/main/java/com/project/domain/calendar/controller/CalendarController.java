package com.project.domain.calendar.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.calendar.domain.RecordType;
import com.project.domain.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
@Log4j2
public class CalendarController {
    private final CalendarService service;

    @GetMapping()
    public ResponseEntity<ApiResponse<?>> getMonthlyRecord(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth date,
            @RequestParam RecordType type
            ) {
        if (type == RecordType.PERSONAL) {
            return ResponseEntity.ok(ApiResponse.success(service.getMonthlyPersonal(date, type)));
        }
        return ResponseEntity.ok(ApiResponse.success(service.getMonthlyWedding(date, type)));
    }
}
