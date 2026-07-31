package com.project.domain.home.service;

import com.project.domain.home.dto.HomeResponseDTO;

public interface HomeService {

    // 인증 사용자의 홈 화면 조회 데이터를 구성합니다.
    HomeResponseDTO getHome(Long authenticatedUserId);
}
