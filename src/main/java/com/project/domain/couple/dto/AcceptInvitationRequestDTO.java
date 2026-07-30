package com.project.domain.couple.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcceptInvitationRequestDTO {

    @NotNull(message = "수락할 상대의 userId는 필수입니다.")
    private Long partnerUserId;
}
