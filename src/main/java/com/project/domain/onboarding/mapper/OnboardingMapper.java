package com.project.domain.onboarding.mapper;

import com.project.domain.onboarding.dto.OnboardingAccountCreateRequestDTO;
import com.project.domain.onboarding.dto.OnboardingAccountRecordCreateRequestDTO;
import com.project.domain.onboarding.dto.OnboardingCardCreateRequestDTO;
import com.project.domain.onboarding.dto.OnboardingCardRecordCreateRequestDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OnboardingMapper {

    // user 테이블에서 사용자의 connected_id 조회
    String findConnectedIdByUserId(@Param("userId") Long userId);

    // user 테이블에 connected_id 업데이트
    int updateUserConnectedId(@Param("userId") Long userId, @Param("connectedId") String connectedId);

    // 계좌(account) 정보 등록
    int insertAccount(OnboardingAccountCreateRequestDTO requestDTO);

    // 카드(card) 정보 등록
    int insertCard(OnboardingCardCreateRequestDTO requestDTO);

    // 계좌 ID (PK) 조회
    Long findAccountIdByUserAndNumber(
            @Param("userId") Long userId,
            @Param("number") String number,
            @Param("company") String company);

    // 카드 ID (PK) 조회
    Long findCardIdByUserAndNumber(
            @Param("userId") Long userId,
            @Param("number") String number,
            @Param("company") String company);

    // 사용자의 특정 금융기관 등록된 계좌 번호 목록 조회
    List<String> findRegisteredAccountNumbers(
            @Param("userId") Long userId,
            @Param("company") String company);

    // 사용자의 특정 금융기관 등록된 카드 번호 목록 조회
    List<String> findRegisteredCardNumbers(
            @Param("userId") Long userId,
            @Param("company") String company);

    // account_record 테이블 거래내역 저장
    int insertAccountRecord(OnboardingAccountRecordCreateRequestDTO requestDTO);

    // card_record 테이블 카드 승인 거래내역 저장
    int insertCardRecord(OnboardingCardRecordCreateRequestDTO requestDTO);
}
