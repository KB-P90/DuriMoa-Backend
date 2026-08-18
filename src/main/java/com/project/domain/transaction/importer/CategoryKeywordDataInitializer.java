package com.project.domain.transaction.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.enums.ExpenseCategory;
import com.project.common.enums.IncomeCategory;
import com.project.domain.transaction.dto.CategoryKeywordConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
@Component
public class CategoryKeywordDataInitializer implements InitializingBean {
    private static final String CATEGORY_FILE = "data/category/category-keywords.json";

    private final ObjectMapper objectMapper;

    private final List<CategoryRule<ExpenseCategory>> expenseRules = new ArrayList<>();

    private final List<CategoryRule<IncomeCategory>> incomeRules = new ArrayList<>();

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public CategoryKeywordDataInitializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterPropertiesSet() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

            Resource[] resources = resolver.getResources(CATEGORY_FILE);

            if (resources.length == 0) {
                throw new IllegalStateException("카테고리 분류 데이터를 찾을 수 없습니다: " + CATEGORY_FILE);
            }

            if (resources.length > 1) {
                throw new IllegalStateException("카테고리 분류 데이터가 여러 개 발견되었습니다: " + CATEGORY_FILE);
            }

            load(resources[0]);

            log.info(
                    "카테고리 분류 데이터 초기화 완료 - expenseRules: {}, incomeRules: {}",
                    expenseRules.size(),
                    incomeRules.size()
            );

        } catch (IOException e) {
            initialized.set(false);

            throw new IllegalStateException("카테고리 분류 데이터 초기화에 실패했습니다.", e);
        }
    }

    private void load(Resource resource) throws IOException {
        try (InputStream inputStream = resource.getInputStream()) {

            CategoryKeywordConfig config =
                    objectMapper.readValue(
                            inputStream,
                            CategoryKeywordConfig.class
                    );

            expenseRules.clear();
            incomeRules.clear();

            expenseRules.addAll(
                    config.getExpense()
                            .stream()
                            .map(this::toExpenseRule)
                            .sorted(
                                    Comparator.comparingInt(
                                            CategoryRule<ExpenseCategory>::getPriority
                                    ).reversed()
                            )
                            .toList()
            );

            incomeRules.addAll(
                    config.getIncome()
                            .stream()
                            .map(this::toIncomeRule)
                            .sorted(
                                    Comparator.comparingInt(
                                            CategoryRule<IncomeCategory>::getPriority
                                    ).reversed()
                            )
                            .toList()
            );
        }
    }

    private CategoryRule<ExpenseCategory> toExpenseRule(
            CategoryKeywordConfig.CategoryRuleConfig config
    ) {
        return new CategoryRule<>(
                ExpenseCategory.valueOf(config.getCategory()),
                config.getPriority(),
                normalizeKeywords(config.getKeywords())
        );
    }

    private CategoryRule<IncomeCategory> toIncomeRule(
            CategoryKeywordConfig.CategoryRuleConfig config
    ) {
        return new CategoryRule<>(
                IncomeCategory.valueOf(config.getCategory()),
                config.getPriority(),
                normalizeKeywords(config.getKeywords())
        );
    }

    private List<String> normalizeKeywords(List<String> keywords) {
        if (keywords == null) {
            return List.of();
        }

        return keywords.stream()
                .map(this::normalize)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        return value
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "")
                .replaceAll("[^가-힣a-z0-9]", "");
    }

    public List<CategoryRule<ExpenseCategory>> getExpenseRules() {
        return List.copyOf(expenseRules);
    }

    public List<CategoryRule<IncomeCategory>> getIncomeRules() {
        return List.copyOf(incomeRules);
    }
}
