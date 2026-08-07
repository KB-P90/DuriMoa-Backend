package com.project.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginUserResponseDTO {

    private Long userId;
    private String phone;
    private String name;
    private String role;
    private String profileImage;
    private String provider;
}
