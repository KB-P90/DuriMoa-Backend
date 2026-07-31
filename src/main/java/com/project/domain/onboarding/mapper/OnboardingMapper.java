package com.project.domain.onboarding.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

@Mapper
public interface OnboardingMapper {

    // user 테이블에서 사용자의 connected_id 조회
    String findConnectedIdByUserId(@Param("userId") Long userId);

    // user 테이블에 connected_id 업데이트
    int updateUserConnectedId(@Param("userId") Long userId, @Param("connectedId") String connectedId);

    // 계좌(account) 정보 등록
    int insertAccount(Map<String, Object> params);

    // 카드(card) 정보 등록
    int insertCard(Map<String, Object> params);

    // 계좌 ID (PK) 조회
    Long findAccountIdByUserAndNumber(
            @Param("userId") Long userId,
            @Param("number") Long number,
            @Param("numberStr") String numberStr,
            @Param("company") String company);

    // 카드 ID (PK) 조회
    Long findCardIdByUserAndNumber(
            @Param("userId") Long userId,
            @Param("number") Long number,
            @Param("numberStr") String numberStr,
            @Param("company") String company);

    // account_record 테이블 거래내역 저장
    int insertAccountRecord(Map<String, Object> params);

    // card_record 테이블 카드 승인 거래내역 저장
    int insertCardRecord(Map<String, Object> params);
}
