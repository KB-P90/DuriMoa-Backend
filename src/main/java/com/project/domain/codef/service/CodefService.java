package com.project.domain.codef.service;

import com.project.domain.codef.dto.CodefAccountListRequestDTO;
import com.project.domain.codef.dto.CodefApiResponseDTO;
import com.project.domain.codef.dto.CodefCardListRequestDTO;
import com.project.domain.codef.dto.CodefConnectedIdRequestDTO;
import com.project.domain.codef.dto.CodefProductRequestDTO;
import com.project.domain.codef.dto.CodefTokenRequestDTO;
import com.project.domain.codef.dto.CodefTokenResponseDTO;

public interface CodefService {

    /**
     * CODEF OAuth2.0 Access Token 발급 및 조회
     */
    CodefTokenResponseDTO getAccessToken(CodefTokenRequestDTO requestDTO);

    /**
     * CODEF Connected ID 생성
     */
    CodefApiResponseDTO<Object> createConnectedId(CodefConnectedIdRequestDTO requestDTO);

    /**
     * CODEF 보유 계좌 목록 조회
     */
    CodefApiResponseDTO<Object> getAccountList(CodefAccountListRequestDTO requestDTO);

    /**
     * CODEF 보유 카드 목록 조회
     */
    CodefApiResponseDTO<Object> getCardList(CodefCardListRequestDTO requestDTO);

    /**
     * CODEF 범용 API 상품 요청
     */
    CodefApiResponseDTO<Object> requestProduct(CodefProductRequestDTO requestDTO);

    /**
     * CODEF Connected ID 계정 추가
     */
    CodefApiResponseDTO<Object> addConnectedAccount(com.project.domain.codef.dto.CodefConnectedIdAddRequestDTO requestDTO);

    /**
     * CODEF Connected ID 계정 정보 수정
     */
    CodefApiResponseDTO<Object> updateConnectedAccount(com.project.domain.codef.dto.CodefConnectedIdUpdateRequestDTO requestDTO);

    /**
     * CODEF Connected ID 계정 삭제
     */
    CodefApiResponseDTO<Object> deleteConnectedAccount(com.project.domain.codef.dto.CodefConnectedIdDeleteRequestDTO requestDTO);

    /**
     * CODEF Connected ID 연동 계정 목록 조회
     */
    CodefApiResponseDTO<Object> getConnectedAccountList(com.project.domain.codef.dto.CodefConnectedIdAccountListRequestDTO requestDTO);

    /**
     * CODEF 커넥티드 아이디 목록 조회
     */
    CodefApiResponseDTO<Object> getConnectedIdList();
}
