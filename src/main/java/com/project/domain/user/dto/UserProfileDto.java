package com.project.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.project.domain.user.domain.UserProfileVO;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {

    private Long id;
    private String name;
    private String role;
    private String provider;
    @JsonIgnore
    private MultipartFile profileImage;
    @JsonProperty("profileImage")
    private String profileImageUrl;
    private String phone;
    private boolean shared;
    private String password;

    private int cardCount;
    private int accountCount;

    private String partnerName;
    private String partnerRole;
    @JsonProperty("partnerProfileImage")
    private String partnerProfileImageUrl;

    @JsonFormat(pattern = "yyyyMMdd")
    private LocalDateTime coupleCreatedAt;
    private String coupleStatus;

    public UserProfileVO toVo() {
        return UserProfileVO.builder()
                .id(id)
                .name(name)
                .role(role)
                .isShared(shared ? 1 : 0)
                .password(password)
                .phone(phone)
                .profileImage(profileImageUrl)
                .build();
    }

}