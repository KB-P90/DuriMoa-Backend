package com.project.domain.codef.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel(description = "CODEF 비밀번호 RSA 암호화 요청 DTO")
public class CodefPasswordEncryptRequestDTO {

    @NotBlank(message = "평문 비밀번호는 필수 항목입니다.")
    @ApiModelProperty(value = "암호화할 평문 비밀번호", required = true, example = "myPassword123!")
    private String plainPassword;

    @ApiModelProperty(value = "CODEF Public Key (미입력시 서버 설정값 사용)")
    private String publicKey;
}
