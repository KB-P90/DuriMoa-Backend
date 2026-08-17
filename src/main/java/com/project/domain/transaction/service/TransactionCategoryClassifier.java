package com.project.domain.transaction.service;

import com.project.common.enums.ExpenseCategory;
import com.project.common.enums.IncomeCategory;
import com.project.domain.transaction.domain.AccountRecord;
import com.project.domain.transaction.domain.CardRecord;
import com.project.domain.transaction.importer.CategoryKeywordDataInitializer;
import com.project.domain.transaction.importer.CategoryRule;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class TransactionCategoryClassifier {

    private final CategoryKeywordDataInitializer dataInitializer;

    public TransactionCategoryClassifier(CategoryKeywordDataInitializer dataInitializer) {
        this.dataInitializer = dataInitializer;
    }

    public ExpenseCategory classifyExpense(CardRecord record) {
        String text = normalize(record.getStoreName());

        return findCategory(text, dataInitializer.getExpenseRules(), ExpenseCategory.ETC);
    }

    public ExpenseCategory classifyExpense(AccountRecord record) {
        String text = normalize(Stream.of(record.getDescription1(), record.getDescription2(), record.getDescription3(), record.getDescription4()).filter(StringUtils::hasText).collect(Collectors.joining(" ")));

        return findCategory(text, dataInitializer.getExpenseRules(), ExpenseCategory.ETC);
    }

    public IncomeCategory classifyIncome(AccountRecord record) {
        String text = normalize(Stream.of(record.getDescription1(), record.getDescription2(), record.getDescription3(), record.getDescription4()).filter(StringUtils::hasText).collect(Collectors.joining(" ")));

        return findCategory(text, dataInitializer.getIncomeRules(), IncomeCategory.ETC);
    }

    private <T> T findCategory(String text, List<CategoryRule<T>> rules, T defaultCategory) {
        if (!StringUtils.hasText(text)) {
            return defaultCategory;
        }

        for (CategoryRule<T> rule : rules) {
            for (String keyword : rule.getKeywords()) {
                if (text.contains(keyword)) {
                    return rule.getCategory();
                }
            }
        }

        return defaultCategory;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        return value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "").replaceAll("[^가-힣a-z0-9]", "");
    }
}
