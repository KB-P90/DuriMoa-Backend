package com.project.domain.mypage.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class LinkedAccountDTO {

    private Long id;
    private String company;
    private String accountName;

    @JsonIgnore
    private String number;

    private String maskedNumber;
}
