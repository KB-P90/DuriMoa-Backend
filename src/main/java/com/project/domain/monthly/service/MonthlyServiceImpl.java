package com.project.domain.monthly.service;

import com.project.domain.monthly.dto.ExpenseCategoryDTO;
import com.project.domain.monthly.dto.MonthlyExpenseResponseDTO;
import com.project.domain.monthly.mapper.MonthlyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MonthlyServiceImpl implements MonthlyService {

    private final MonthlyMapper monthlyMapper;

    @Override
    @Transactional(readOnly = true)
    public MonthlyExpenseResponseDTO getMonthlyCategoryExpenses(Long userId, Integer year, Integer month) {

        List<ExpenseCategoryDTO> categories = monthlyMapper.selectMonthlyExpenseCategories(userId, year, month);
        
        if (categories == null) {
            categories = Collections.emptyList();
        }

        return MonthlyExpenseResponseDTO.builder()
                .year(year)
                .expenseCategories(categories)
                .build();
    }
}
