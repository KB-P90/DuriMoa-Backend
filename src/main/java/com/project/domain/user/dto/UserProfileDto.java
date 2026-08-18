package com.project.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

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
    @JsonProperty("profileImage")
    private String profileImageUrl;
    private String phone;
    private boolean shared;

    private int cardCount;
    private int accountCount;

    private String partnerName;
    private String partnerRole;
    @JsonProperty("partnerProfileImage")
    private String partnerProfileImageUrl;

    @JsonFormat(pattern = "yyyyMMdd")
    private LocalDateTime coupleCreatedAt;
    private String coupleStatus;

}
