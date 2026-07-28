package com.project.domain.calendar.controller;

import com.project.domain.calendar.service.CalendarService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/record")
@RequiredArgsConstructor
@Log4j2
public class CalendarController {
    private final CalendarService service;
}
