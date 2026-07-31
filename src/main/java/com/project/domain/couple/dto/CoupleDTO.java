package com.project.domain.couple.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CoupleDTO {

    private Long id;
    private Long groomId;
    private Long brideId;
    private String status;

    public boolean hasMember(Long userId) {
        return userId != null && (userId.equals(groomId) || userId.equals(brideId));
    }
}
