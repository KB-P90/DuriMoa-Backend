package com.project.domain.mypage.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LinkedAssetsResponseDTO {

    private final List<LinkedAccountDTO> accounts;
    private final List<LinkedCardDTO> cards;
}
