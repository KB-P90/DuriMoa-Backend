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
    private String name;
    private String role;
    private byte[] profileImage;
    private String provider;
    private Integer isShared;
}
