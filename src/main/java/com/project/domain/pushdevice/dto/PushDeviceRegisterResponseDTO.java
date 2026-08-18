package com.project.domain.pushdevice.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "푸시 기기 등록 응답")
public class PushDeviceRegisterResponseDTO {

    @ApiModelProperty(value = "기기 ID", example = "1")
    private Long deviceId;

    @ApiModelProperty(value = "기기 플랫폼", example = "ANDROID")
    private String platform;

    @ApiModelProperty(value = "푸시 활성화 여부", example = "true")
    private Boolean pushEnabled;
}
