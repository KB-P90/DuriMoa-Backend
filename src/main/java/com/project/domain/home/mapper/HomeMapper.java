package com.project.domain.home.mapper;

import com.project.domain.home.model.HomeRecord.HomeAssetSummaryRecord;
import com.project.domain.home.model.HomeRecord.HomeBudgetPreviewRecord;
import com.project.domain.home.model.HomeRecord.HomeChecklistEvidenceRecord;
import com.project.domain.home.model.HomeRecord.HomeCoupleRecord;
import com.project.domain.home.model.HomeRecord.HomeSavingMissionSummaryRecord;
import com.project.domain.home.model.HomeRecord.HomeUserRecord;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HomeMapper {

    // 인증 사용자 기본 정보를 조회합니다.
    HomeUserRecord selectHomeUser(@Param("userId") Long userId);

    // 인증 사용자가 속한 활성 커플 정보를 조회합니다.
    HomeCoupleRecord selectActiveCouple(@Param("userId") Long userId);

    // 인증 사용자에게 연결 계좌 또는 카드가 있는지 조회합니다.
    Boolean selectAccountLinked(@Param("userId") Long userId);

    // 홈 준비 체크리스트의 실제 수행 근거를 조회합니다.
    HomeChecklistEvidenceRecord selectChecklistEvidence(
            @Param("userId") Long userId,
            @Param("coupleId") Long coupleId,
            @Param("groomId") Long groomId,
            @Param("brideId") Long brideId
    );

    // 메인 홈의 예산 달성률과 자산 요약에 필요한 금액을 조회합니다.
    HomeAssetSummaryRecord selectAssetSummary(
            @Param("userId") Long userId,
            @Param("groomId") Long groomId,
            @Param("brideId") Long brideId,
            @Param("goalId") Long goalId
    );

    // 체크리스트 미완료 홈의 지역별 예상 예산 미리보기 목록을 조회합니다.
    List<HomeBudgetPreviewRecord> selectExpectedBudgetPreviewRegions();

    // 현재 월 홈 도전중 절약미션 요약과 대표 미션을 조회합니다.
    HomeSavingMissionSummaryRecord selectSavingMissionSummary(
            @Param("userId") Long userId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );
}
