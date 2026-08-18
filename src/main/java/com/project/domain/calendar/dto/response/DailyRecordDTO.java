package com.project.domain.calendar.dto.response;

import com.project.common.enums.RecordType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "일별 거래 내역 상세 DTO")
public class DailyRecordDTO {

    @ApiModelProperty(value = "거래 내역 ID", example = "1")
    private Long recordId;

    @ApiModelProperty(value = "거래 유형", example = "EXPENSE")
    private RecordType type;

    @ApiModelProperty(value = "거래 카테고리", example = "식비")
    private String category;

    @ApiModelProperty(value = "거래 금액", example = "50000")
    private Long amount;

    @ApiModelProperty(value = "거래 메모", example = "점심 식사")
    private String memo;

    @ApiModelProperty(value = "거래 소유자", example = "나")
    private String owner;

    @ApiModelProperty(value = "거래 소유자 id", example = "1")
    private Long ownerId;

    @ApiModelProperty(value = "거래 소유자 여부", example = "false")
    private boolean isMine;

    @ApiModelProperty(value = "사용자 직접 추가 여부", example = "false")
    private boolean isUserCreated;
}
