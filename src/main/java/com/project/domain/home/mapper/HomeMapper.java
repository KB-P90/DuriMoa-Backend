package com.project.domain.home.mapper;

import com.project.domain.home.model.HomeRecord.HomeChecklistEvidenceRecord;
import com.project.domain.home.model.HomeRecord.HomeCoupleRecord;
import com.project.domain.home.model.HomeRecord.HomeSavingMissionSummaryRecord;
import com.project.domain.home.model.HomeRecord.HomeUserRecord;
import org.apache.ibatis.annotations.Param;

public interface HomeMapper {

    // 인증 사용자 기본 정보를 조회합니다.
    HomeUserRecord selectHomeUser(@Param("userId") Long userId);

    // 인증 사용자가 속한 활성 커플 정보를 조회합니다.
    HomeCoupleRecord selectActiveCouple(@Param("userId") Long userId);

    // 홈 준비 체크리스트의 실제 수행 근거를 조회합니다.
    HomeChecklistEvidenceRecord selectChecklistEvidence(
            @Param("userId") Long userId,
            @Param("coupleId") Long coupleId,
            @Param("groomId") Long groomId,
            @Param("brideId") Long brideId
    );

    // 현재 월 홈 도전중 절약미션 요약과 대표 미션을 조회합니다.
    HomeSavingMissionSummaryRecord selectSavingMissionSummary(
            @Param("userId") Long userId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );
}
