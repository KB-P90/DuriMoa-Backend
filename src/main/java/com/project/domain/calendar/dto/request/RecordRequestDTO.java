package com.project.domain.calendar.dto.request;

import com.project.common.enums.ExpenseCategory;
import com.project.common.enums.IncomeCategory;
import com.project.common.enums.RecordType;
import com.project.common.enums.SavingCategory;
import com.project.domain.calendar.domain.ServiceRecord;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordRequestDTO {
    RecordType type;
    String category;
    Long amount;
    LocalDate recordDate;
    String memo;

    public ServiceRecord toVO(Long userId) {
        String categoryCode = switch (type) {
            case INCOME -> IncomeCategory.fromKorean(category).name();
            case EXPENSE -> ExpenseCategory.fromKorean(category).name();
            case SAVING -> SavingCategory.fromKorean(category).name();
        };

        return ServiceRecord.builder()
                .userId(userId)
                .type(type)
                .category(categoryCode)
                .amount(amount)
                .recordDate(recordDate)
                .memo(memo)
                .build();
    }
}
