package com.project.domain.codef.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "CODEF 비밀번호 RSA 암호화 응답 DTO")
public class CodefPasswordEncryptResponseDTO {

    @ApiModelProperty(value = "RSA 암호화된 비밀번호 (Base64)")
    private String encryptedPassword;
}
