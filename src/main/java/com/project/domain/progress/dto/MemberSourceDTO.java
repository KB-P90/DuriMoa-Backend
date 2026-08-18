package com.project.domain.progress.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberSourceDTO {

    private Long userId;
    private String name;
    private String role;
    private String profileImage;
    private Long asset;
    private Boolean assetShared;
}
