package com.project.domain.monthly.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.monthly.dto.MonthlyExpenseSummaryCacheDTO;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Log4j2
@Service
public class MonthlyExpenseCacheService {

    private static final String KEY_PREFIX = "monthly:expense-summary:v1:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Boolean cacheEnabled;
    private final Duration currentMonthTtl;
    private final Duration pastMonthTtl;
    private final Counter cacheHitCounter;
    private final Counter cacheMissCounter;
    private final Counter cacheErrorCounter;
    private final Counter cacheEvictionCounter;
    private final Counter databaseAggregateQueryCounter;

    public MonthlyExpenseCacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            @Value("${cache.monthly-expense.enabled:true}") Boolean cacheEnabled,
            @Value("${cache.monthly-expense.current-month-ttl-seconds:300}") Long currentMonthTtlSeconds,
            @Value("${cache.monthly-expense.past-month-ttl-seconds:86400}") Long pastMonthTtlSeconds) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheEnabled = cacheEnabled;
        this.currentMonthTtl = Duration.ofSeconds(currentMonthTtlSeconds);
        this.pastMonthTtl = Duration.ofSeconds(pastMonthTtlSeconds);
        this.cacheHitCounter = meterRegistry.counter("monthly.expense.cache", "result", "hit");
        this.cacheMissCounter = meterRegistry.counter("monthly.expense.cache", "result", "miss");
        this.cacheErrorCounter = meterRegistry.counter("monthly.expense.cache", "result", "error");
        this.cacheEvictionCounter = meterRegistry.counter("monthly.expense.cache.eviction");
        this.databaseAggregateQueryCounter = meterRegistry.counter(
                "monthly.expense.db.aggregate.queries");
    }

    public Optional<MonthlyExpenseSummaryCacheDTO> find(Long userId, YearMonth yearMonth) {
        if (!cacheEnabled) {
            return Optional.empty();
        }
        String key = createKey(userId, yearMonth);
        try {
            String cachedValue = redisTemplate.opsForValue().get(key);
            if (cachedValue == null) {
                cacheMissCounter.increment();
                return Optional.empty();
            }

            MonthlyExpenseSummaryCacheDTO cachedSummary = objectMapper.readValue(
                    cachedValue,
                    MonthlyExpenseSummaryCacheDTO.class);
            cacheHitCounter.increment();
            return Optional.of(cachedSummary);
        } catch (RuntimeException | JsonProcessingException exception) {
            cacheErrorCounter.increment();
            log.warn("월별 지출 캐시 조회에 실패하여 DB 조회로 대체합니다. userId: {}, yearMonth: {}",
                    userId, yearMonth, exception);
            return Optional.empty();
        }
    }

    public void save(Long userId, YearMonth yearMonth, MonthlyExpenseSummaryCacheDTO summary) {
        if (!cacheEnabled) {
            return;
        }
        try {
            String serializedSummary = objectMapper.writeValueAsString(summary);
            redisTemplate.opsForValue().set(
                    createKey(userId, yearMonth),
                    serializedSummary,
                    resolveTtl(yearMonth));
        } catch (RuntimeException | JsonProcessingException exception) {
            cacheErrorCounter.increment();
            log.warn("월별 지출 캐시 저장에 실패했습니다. userId: {}, yearMonth: {}",
                    userId, yearMonth, exception);
        }
    }

    public void evict(Long userId, YearMonth yearMonth) {
        if (!cacheEnabled) {
            return;
        }
        try {
            Boolean deleted = redisTemplate.delete(createKey(userId, yearMonth));
            if (Boolean.TRUE.equals(deleted)) {
                cacheEvictionCounter.increment();
            }
        } catch (RuntimeException exception) {
            cacheErrorCounter.increment();
            log.warn("월별 지출 캐시 삭제에 실패했습니다. userId: {}, yearMonth: {}",
                    userId, yearMonth, exception);
        }
    }

    public void evictAfterCommit(Long userId, YearMonth yearMonth) {
        executeAfterCommit(() -> evict(userId, yearMonth));
    }

    public void evictAll(Long userId) {
        if (!cacheEnabled) {
            return;
        }
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(KEY_PREFIX + userId + ":*")
                .count(100)
                .build();
        List<String> keys = new ArrayList<>();

        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            cursor.forEachRemaining(keys::add);
            if (!keys.isEmpty()) {
                Long deletedCount = redisTemplate.delete(keys);
                if (deletedCount != null && deletedCount > 0) {
                    cacheEvictionCounter.increment(deletedCount);
                }
            }
        } catch (RuntimeException exception) {
            cacheErrorCounter.increment();
            log.warn("사용자의 월별 지출 캐시 전체 삭제에 실패했습니다. userId: {}", userId, exception);
        }
    }

    public void evictAllAfterCommit(Long userId) {
        executeAfterCommit(() -> evictAll(userId));
    }

    public void recordDatabaseAggregateQueries(Integer queryCount) {
        databaseAggregateQueryCounter.increment(queryCount);
    }

    private void executeAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        action.run();
                    }
                });
    }

    private Duration resolveTtl(YearMonth yearMonth) {
        return YearMonth.now().equals(yearMonth) ? currentMonthTtl : pastMonthTtl;
    }

    private String createKey(Long userId, YearMonth yearMonth) {
        return KEY_PREFIX + userId + ":" + yearMonth;
    }
}
