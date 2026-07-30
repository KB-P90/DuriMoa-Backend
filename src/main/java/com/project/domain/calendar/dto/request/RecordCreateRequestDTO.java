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
public class RecordCreateRequestDTO {
    RecordType type;
    String category;
    Long amount;
    LocalDate recordDate;
    String memo;

    public static RecordCreateRequestDTO of(ServiceRecord record) {
        return record == null ? null : RecordCreateRequestDTO.builder()
                .type(record.getType())
                .category(record.getCategory())
                .amount(record.getAmount())
                .recordDate(record.getRecordDate())
                .memo(record.getMemo())
                .build();
    }

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
