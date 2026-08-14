package com.project.domain.pushdevice.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "푸시 기기 등록 요청")
public class PushDeviceRegisterRequestDTO {

    @NotBlank(message = "FCM 토큰은 필수입니다.")
    @ApiModelProperty(value = "FCM 디바이스 토큰", required = true, example = "d1qA2b...")
    private String token;

    @NotBlank(message = "플랫폼은 필수입니다.")
    @Size(max = 20, message = "플랫폼은 20자 이하여야 합니다.")
    @ApiModelProperty(value = "기기 플랫폼", required = true, example = "ANDROID")
    private String platform;
}
