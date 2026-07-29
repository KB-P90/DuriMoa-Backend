package com.project.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LoginUserDTO {

    private Long userId;
    private String phone;
    private String password;
    private String name;
    private String role;
    private byte[] profileImage;
    private String provider;
}
