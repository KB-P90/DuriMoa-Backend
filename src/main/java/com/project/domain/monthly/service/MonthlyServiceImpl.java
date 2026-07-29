package com.project.domain.monthly.service;

import com.project.domain.monthly.dto.ExpenseCategoryDTO;
import com.project.domain.monthly.dto.MonthlyExpenseResponseDTO;
import com.project.domain.monthly.dto.SavingMissionChallengeRequestDTO;
import com.project.domain.monthly.dto.SavingMissionDTO;
import com.project.domain.monthly.dto.SavingMissionResponseDTO;
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

    @Override
    @Transactional(readOnly = true)
    public SavingMissionResponseDTO getMonthlySavingMissions(Long userId, Integer year, Integer month) {
        List<SavingMissionDTO> missions = monthlyMapper.selectMonthlySavingMissions(userId, year, month);

        if (missions == null) {
            missions = Collections.emptyList();
        }

        long totalExpectedSavingAmount = missions.stream()
                .mapToLong(m -> m.getExpectedSavingAmount() != null ? m.getExpectedSavingAmount() : 0L)
                .sum();

        String yearMonth = String.format("%04d-%02d", year, month);

        return SavingMissionResponseDTO.builder()
                .yearMonth(yearMonth)
                .totalExpectedSavingAmount(totalExpectedSavingAmount)
                .missions(missions)
                .build();
    }

    @Override
    @Transactional
    public void startSavingMission(Long userId, Long missionId, SavingMissionChallengeRequestDTO requestDTO) {
        monthlyMapper.updateSavingMissionStatusToInProgress(userId, missionId);
    }
}
