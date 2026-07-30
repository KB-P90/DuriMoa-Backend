package com.project.domain.couple.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CoupleDTO {

    private Long userId;
    private String name;
    private String role;
    private String status;
}
