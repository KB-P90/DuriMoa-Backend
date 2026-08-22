package com.project.domain.transaction.dto;

import com.project.domain.calendar.domain.ServiceRecord;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "CODEF 거래내역 원본 동기화 결과 DTO")
public class TransactionSyncResponseDTO {

    @ApiModelProperty(value = "처리한 CODEF 거래 수")
    private Integer fetchedCount;

    @ApiModelProperty(value = "새로 저장한 원본 거래 수")
    private Integer savedCount;

    @ApiModelProperty(value = "이미 저장되어 건너뛴 거래 수")
    private Integer duplicateCount;

    @ApiModelProperty(value = "조회한 계좌 수")
    private Integer accountCount;

    @ApiModelProperty(value = "조회한 카드 수")
    private Integer cardCount;

    @ApiModelProperty(value = "이번 동기화로 새로 생성된 지출 서비스 레코드 목록")
    @Builder.Default
    private List<ServiceRecord> newExpenseRecords = Collections.emptyList();
}
