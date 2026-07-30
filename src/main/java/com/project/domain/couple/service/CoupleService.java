package com.project.domain.couple.service;

import com.project.domain.couple.dto.CoupleDTO;

public interface CoupleService {

    CoupleDTO getConnectedCoupleForCurrentUser();

    CoupleDTO getConnectedCoupleByUserId(Long userId);
}
