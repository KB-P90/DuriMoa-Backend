package com.project.domain.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    private byte[] profileImage;
    private boolean shared;

    // 계좌정보 dto 생성 된 후 추가할 예정

    private String partnerName;
    private String partnerRole;

    @JsonFormat(pattern = "yyyyMMdd")
    private LocalDateTime coupleCreatedAt;
    private String coupleStatus;

}
