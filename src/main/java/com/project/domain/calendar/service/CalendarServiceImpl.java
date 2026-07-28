package com.project.domain.calendar.service;

import com.project.domain.calendar.mapper.CalendarMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class CalendarServiceImpl implements CalendarService {
    final private CalendarMapper mapper;
}
