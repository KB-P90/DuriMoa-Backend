package com.project.domain.calendar.service;

import com.project.common.enums.ExpenseCategory;
import com.project.common.enums.IncomeCategory;
import com.project.common.enums.SavingCategory;
import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.common.util.CategoryConverter;
import com.project.domain.calendar.domain.CalendarType;
import com.project.domain.calendar.domain.ServiceRecord;
import com.project.domain.calendar.dto.request.RecordRequestDTO;
import com.project.domain.calendar.dto.response.*;
import com.project.domain.calendar.mapper.CalendarMapper;
import com.project.domain.transaction.service.TransactionSyncAsyncService;
import com.project.domain.transaction.service.TransactionSyncService;
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
    final private TransactionSyncService transactionSyncService;
    final private TransactionSyncAsyncService transactionSyncAsyncService;

    @Override
    public Object getMonthly(Long userId, YearMonth month, CalendarType type) {
        if (transactionSyncService.needSync(userId) && transactionSyncAsyncService.tryStarySync(userId)) {
            transactionSyncAsyncService.syncUserTransactionsAsync(userId);
        }

        return switch (type) {
            case PERSONAL -> getMonthlyPersonal(userId, month);
            case WEDDING -> getMonthlyWedding(userId, month);
        };
    }

    // 월별 조회 - 개인 소비
    private MonthlyPersonalResponseDTO getMonthlyPersonal(Long userId, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        List<MonthlyRecordAggregateDTO> aggregates =
                mapper.selectMonthlyPersonalAggregates(userId, start, end);

        MonthlyPersonalSummaryDTO summary = MonthlyPersonalSummaryDTO.builder()
                .income(sumAmount(aggregates, MonthlyRecordAggregateDTO::getIncome))
                .expense(sumAmount(aggregates, MonthlyRecordAggregateDTO::getExpense))
                .saving(sumAmount(aggregates, MonthlyRecordAggregateDTO::getSaving))
                .build();

        List<CalendarDayDTO> calendar = buildCalendarFromAggregates(start, end, aggregates);

        return new MonthlyPersonalResponseDTO(summary, calendar);
    }

    // 월별 조회 - 결혼 비용
    private MonthlyWeddingResponseDTO getMonthlyWedding(Long userId, YearMonth month) {
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        // 결혼 카테고리
        List<String> categories = getWeddingCategories();

        List<MonthlyRecordAggregateDTO> aggregates =
                mapper.selectMonthlyWeddingAggregates(userId, start, end, categories);

        MonthlyWeddingSummaryDTO summary = MonthlyWeddingSummaryDTO.builder()
                .goalAmount(mapper.selectWeddingGoalAmount(userId))
                .expense(sumAmount(aggregates, MonthlyRecordAggregateDTO::getExpense))
                .saving(sumAmount(aggregates, MonthlyRecordAggregateDTO::getSaving))
                .build();

        List<CalendarDayDTO> calendar = buildCalendarFromAggregates(start, end, aggregates);

        return new MonthlyWeddingResponseDTO(summary, calendar);
    }

    private Long sumAmount(List<MonthlyRecordAggregateDTO> aggregates,
                           Function<MonthlyRecordAggregateDTO, Long> amountGetter) {
        return aggregates.stream()
                .map(amountGetter)
                .filter(amount -> amount != null)
                .mapToLong(Long::longValue)
                .sum();
    }

    private List<CalendarDayDTO> buildCalendarFromAggregates(LocalDate start, LocalDate end,
                                                             List<MonthlyRecordAggregateDTO> result) {
        Map<LocalDate, CalendarDayDTO> map = result.stream()
                .collect(Collectors.toMap(
                        MonthlyRecordAggregateDTO::getDate,
                        aggregate -> CalendarDayDTO.builder()
                                .date(aggregate.getDate())
                                .hasIncome(Boolean.TRUE.equals(aggregate.getHasIncome()))
                                .hasExpense(Boolean.TRUE.equals(aggregate.getHasExpense()))
                                .hasSaving(Boolean.TRUE.equals(aggregate.getHasSaving()))
                                .build()
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
        return switch (type) {
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

    @Override
    public RecordResponseDTO create(Long userId, RecordRequestDTO recordDTO) {

        ServiceRecord record = recordDTO.toVO(userId);
        mapper.create(record);

        return RecordResponseDTO.of(record.getId());
    }

    @Override
    public RecordResponseDTO update(Long userId, Long recordId, RecordRequestDTO recordDTO) {

        ServiceRecord existingRecord = mapper.findById(recordId);

        if (existingRecord == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND);
        }

        if (!existingRecord.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 사용자가 직접 추가한 거래 내역인지, CODEF 연동 거래 내역인지 확인
        boolean isUserCreated = existingRecord.getAccountRecordId() == null && existingRecord.getCardRecordId() == null;

        ServiceRecord record = recordDTO.toVO(userId);
        record.setId(recordId);

        // CODEF 연동 거래는 카테고리와 메모만 수정 가능
        if (!isUserCreated) {
            record.setAmount(existingRecord.getAmount());
            record.setType(existingRecord.getType());
            record.setRecordDate(existingRecord.getRecordDate());
        }

        mapper.update(record);

        return RecordResponseDTO.of(record.getId());
    }

    @Override
    public RecordResponseDTO delete(Long userId, Long recordId) {
        int result = mapper.delete(userId, recordId);

        if (result == 0) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        return RecordResponseDTO.of(recordId);
    }
}
