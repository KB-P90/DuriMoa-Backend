package com.project.domain.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileVO {
    private Long id;
    private String phone;
    private String password;
    private String name;
    private String role;
    private String profileImage;
    private Integer isShared;
}
