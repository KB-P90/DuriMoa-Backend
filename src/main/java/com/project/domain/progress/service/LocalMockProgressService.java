package com.project.domain.progress.service;

import com.project.domain.progress.dto.GoalChecklistItemDTO;
import com.project.domain.progress.dto.IndividualProgressDTO;
import com.project.domain.progress.dto.MonthlySavingDTO;
import com.project.domain.progress.dto.MonthlySuccessRecordDTO;
import com.project.domain.progress.dto.ProgressResponseDTO;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("local")
public class LocalMockProgressService implements ProgressService {

    @Override
    public ProgressResponseDTO getProgress() {
        return ProgressResponseDTO.builder()
                .totalSavingAmount(31_100_000L)
                .targetAmount(50_000_000L)
                .progressRate(62)
                .groomSavingAmount(14_670_000L)
                .brideSavingAmount(16_430_000L)
                .monthlySavings(List.of(
                        new MonthlySavingDTO("2026-02", 5_000_000L),
                        new MonthlySavingDTO("2026-03", 2_000_000L),
                        new MonthlySavingDTO("2026-05", 7_000_000L),
                        new MonthlySavingDTO("2026-07", 17_100_000L)))
                .goalChecklist(List.of(
                        new GoalChecklistItemDTO("예식장", 10_800_000L, 10_500_000L, 97, true),
                        new GoalChecklistItemDTO("스튜디오", 3_800_000L, 3_800_000L, 100, true),
                        new GoalChecklistItemDTO("드레스", 5_200_000L, null, null, false),
                        new GoalChecklistItemDTO("메이크업", 1_800_000L, null, null, false),
                        new GoalChecklistItemDTO("예물", 3_200_000L, null, null, false)))
                .individualProgress(List.of(
                        new IndividualProgressDTO("김현준", 14_670_000L, 25_000_000L, 58),
                        new IndividualProgressDTO("이서연", 16_430_000L, 25_000_000L, 65)))
                .monthlySuccessRecords(List.of(
                        new MonthlySuccessRecordDTO("2026.07", 79, 12_000_000L, null),
                        new MonthlySuccessRecordDTO("2026.06", 51, 7_200_000L, 6),
                        new MonthlySuccessRecordDTO("2026.05", 36, 3_500_000L, 6),
                        new MonthlySuccessRecordDTO("2026.04", 18, 2_700_000L, 6),
                        new MonthlySuccessRecordDTO("2026.03", 11, 1_500_000L, 3),
                        new MonthlySuccessRecordDTO("2026.02", 8, 300_000L, null)))
                .build();
    }
}
