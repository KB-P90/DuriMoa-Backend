package com.project.domain.calendar.service;

import com.project.common.enums.ExpenseCategory;
import com.project.common.enums.IncomeCategory;
import com.project.common.enums.SavingCategory;
import com.project.common.util.CategoryConverter;
import com.project.domain.calendar.domain.CalendarType;
import com.project.domain.calendar.dto.response.*;
import com.project.domain.calendar.mapper.CalendarMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class CalendarServiceImpl implements CalendarService {
    final private CalendarMapper mapper;

    @Override
    public Object getMonthly(Long userId, YearMonth month, CalendarType type) {
        return switch(type) {
            case PERSONAL -> getMonthlyPersonal(userId, month);
            case WEDDING -> getMonthlyWedding(userId, month);
        };
    }

    // 월별 조회 - 개인 소비
    private MonthlyPersonalResponseDTO getMonthlyPersonal(Long userId, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = getEndDate(month);

        // 월 요약
        MonthlyPersonalSummaryDTO summary = mapper.selectMonthlyPersonalSummary(userId, start, end);

        // 달력 조회
        List<CalendarDayDTO> calendar = buildCalendar(start, end, mapper.selectMonthlyPersonalCalendar(userId, start, end));

        return new MonthlyPersonalResponseDTO(summary, calendar);
    }

    // 월별 조회 - 결혼 비용
    private MonthlyWeddingResponseDTO getMonthlyWedding(Long userId, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = getEndDate(month);

        // 결혼 카테고리
        List<String> categories = getWeddingCategories();
        
        // 월 요약
        MonthlyWeddingSummaryDTO summary = mapper.selectMonthlyWeddingSummary(userId, start, end, categories);

        // TODO Goal 기능 병합 후 GoalMapper로 교체
        summary.setGoalAmount(0L);

        // 달력 조회
        List<CalendarDayDTO> calendar = buildCalendar(start, end, mapper.selectMonthlyWeddingCalendar(userId, start, end, categories));

        return new MonthlyWeddingResponseDTO(summary, calendar);
    }

    // 해당 월 마지막 날짜 계산 - 현재 달일 경우 오늘까지 조회
    private LocalDate getEndDate(YearMonth month) {
        LocalDate today = LocalDate.now();
        return month.equals(YearMonth.from(today)) ? today : month.atEndOfMonth();
    }

    // 거래 내역 없어서 비어있는 날짜 있으면 데이터 추가
    private List<CalendarDayDTO> buildCalendar(LocalDate start, LocalDate end, List<CalendarDayDTO> result) {
        Map<LocalDate, CalendarDayDTO> map = result.stream()
                .collect(Collectors.toMap(
                        CalendarDayDTO::getDate,
                        Function.identity()
                ));

        List<CalendarDayDTO> calendar = new ArrayList<>();

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            CalendarDayDTO dto = map.get(d);

            if (dto == null) {
                dto = new CalendarDayDTO();
                dto.setDate(d);
                dto.setHasIncome(false);
                dto.setHasExpense(false);
                dto.setHasSaving(false);
            }

            calendar.add(dto);
        }

        return calendar;
    }

    @Override
    public DailyResponseDTO getDaily(Long userId, LocalDate date, CalendarType type) {
        return switch(type) {
            case PERSONAL -> getDailyPersonal(userId, date);
            case WEDDING -> getDailyWedding(userId, date);
        };
    }

    // 일별 조회 - 개인 소비
    private DailyResponseDTO getDailyPersonal(Long userId, LocalDate date) {
        List<DailyRecordDTO> records = mapper.selectDailyPersonal(userId, date);

        return new DailyResponseDTO(date, records.size(), convertCategory(records));
    }

    // 일별 조회 - 결혼 비용
    private DailyResponseDTO getDailyWedding(Long userId, LocalDate date) {

        // 결혼 카테고리
        List<String> categories = getWeddingCategories();

        List<DailyRecordDTO> records = mapper.selectDailyWedding(userId, date, categories);

        return new DailyResponseDTO(date, records.size(), convertCategory(records));
    }

    // 거래 내역의 영문 카테고리를 한글로 변환
    private List<DailyRecordDTO> convertCategory(List<DailyRecordDTO> records) {
        records.forEach(record -> {
            record.setCategory(
                    CategoryConverter.convert(record.getType(), record.getCategory())
            );
        });

        return records;
    }

    // 결혼 카테고리 리스트
    private List<String> getWeddingCategories() {
        List<String> categories = new ArrayList<>();

        categories.addAll(ExpenseCategory.getWeddingCodes());
        categories.addAll(IncomeCategory.getWeddingCodes());
        categories.addAll(SavingCategory.getWeddingCodes());

        return categories;
    }
}
