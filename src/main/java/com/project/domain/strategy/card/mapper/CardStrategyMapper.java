package com.project.domain.strategy.card.mapper;

import com.project.domain.strategy.card.model.CardStrategyRecord.CardOwnerRecord;
import com.project.domain.strategy.card.model.CardStrategyRecord.CardPerformanceRecord;
import com.project.domain.strategy.card.model.CardStrategyRecord.CardProductRecord;
import com.project.domain.strategy.card.model.CardStrategyRecord.CardProductSource;
import com.project.domain.strategy.card.model.CardStrategyRecord.UserCardRecord;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CardStrategyMapper {

    // 인증 사용자가 속한 활성 커플의 구성원을 조회합니다.
    List<CardOwnerRecord> selectActiveCoupleMembers(@Param("userId") Long userId);

    // 활성 커플 구성원의 연결 카드와 카드 상품 정보를 조회합니다.
    List<UserCardRecord> selectUserCardsByUserIds(@Param("userIds") List<Long> userIds);

    // 카드별 현재 월 인정 실적을 조회합니다.
    List<CardPerformanceRecord> selectCurrentMonthPerformances(
            @Param("cardIds") List<Long> cardIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // 사용자 카드 식별자로 카드 상품 상세 정보를 조회합니다.
    CardProductRecord selectCardDetailByCardId(@Param("cardId") Long cardId);

    // 인증 사용자의 활성 커플 안에서 접근 가능한 카드인지 확인합니다.
    Integer existsCardAccessInActiveCouple(
            @Param("userId") Long userId,
            @Param("cardId") Long cardId
    );

    // 카드사와 상품명 기준으로 기존 카드 상품 ID를 조회합니다.
    Long selectCardProductIdByCompanyAndName(
            @Param("company") String company,
            @Param("name") String name
    );

    // 신규 카드 상품 정보를 등록합니다.
    Integer insertCardProduct(@Param("source") CardProductSource source);

    // 기존 카드 상품 정보를 카드사와 상품명 기준으로 갱신합니다.
    Integer updateCardProductByCompanyAndName(@Param("source") CardProductSource source);
}
