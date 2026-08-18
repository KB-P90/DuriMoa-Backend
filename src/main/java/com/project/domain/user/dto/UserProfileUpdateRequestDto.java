package com.project.domain.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
public class UserProfileUpdateRequestDto {

    private String name;
    private String role;
    private String phone;
    private MultipartFile profileImage;
    private Boolean removeProfileImage = Boolean.FALSE;
    private String password;
    private String currentPassword;
}
