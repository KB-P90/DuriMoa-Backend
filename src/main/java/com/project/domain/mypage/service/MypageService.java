package com.project.domain.mypage.service;

import com.project.domain.mypage.dto.LinkedAssetsResponseDTO;

public interface MypageService {

    LinkedAssetsResponseDTO getLinkedAssets(Long userId);
}
