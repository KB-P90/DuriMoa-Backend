package com.project.domain.calendar.dto.request;

import com.project.common.enums.ExpenseCategory;
import com.project.common.enums.IncomeCategory;
import com.project.common.enums.RecordType;
import com.project.common.enums.SavingCategory;
import com.project.domain.calendar.domain.ServiceRecord;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ApiModel(description = "거래 내역 등록 및 수정 요청 DTO")
public class RecordRequestDTO {

    @ApiModelProperty(value = "거래 유형", example = "EXPENSE", required = true)
    RecordType type;

    @ApiModelProperty(value = "거래 카테고리", example = "식비", required = true)
    String category;

    @ApiModelProperty(value = "거래 금액", example = "50000", required = true)
    Long amount;

    @ApiModelProperty(value = "거래 날짜", example = "2026-07-30", required = true)
    LocalDate recordDate;

    @ApiModelProperty(value = "거래 메모", example = "웨딩홀 계약금", required = false)
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
