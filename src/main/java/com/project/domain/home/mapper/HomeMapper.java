package com.project.domain.home.mapper;

import com.project.domain.home.model.HomeRecord.HomeAssetSummaryRecord;
import com.project.domain.home.model.HomeRecord.HomeAssetRecord;
import com.project.domain.home.model.HomeRecord.HomeBudgetPreviewRecord;
import com.project.domain.home.model.HomeRecord.HomeChecklistEvidenceRecord;
import com.project.domain.home.model.HomeRecord.HomeCoupleRecord;
import com.project.domain.home.model.HomeRecord.HomeMemberAssetRecord;
import com.project.domain.home.model.HomeRecord.HomeSavingAlertRecord;
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

    // 홈 자산 요약 계산에 사용할 활성 커플 구성원의 자산 공개 정보를 조회합니다.
    List<HomeMemberAssetRecord> selectHomeMemberAssets(
            @Param("groomId") Long groomId,
            @Param("brideId") Long brideId
    );

    // 홈 자산 요약 계산에 사용할 거래 금액 원본을 조회합니다.
    List<HomeAssetRecord> selectAssetRecords(
            @Param("groomId") Long groomId,
            @Param("brideId") Long brideId
    );

    // 체크리스트 미완료 홈의 지역별 예상 예산 미리보기 목록을 조회합니다.
    List<HomeBudgetPreviewRecord> selectExpectedBudgetPreviewRegions();

    // 홈 알림 배너에 표시할 인증 사용자의 최신 알림 1건을 조회합니다.
    HomeSavingAlertRecord selectLatestSavingAlert(@Param("userId") Long userId);

    // 홈 상단 알림 버튼에 표시할 읽지 않은 알림 개수를 조회합니다.
    Integer countUnreadNotifications(@Param("userId") Long userId);

    // 현재 월 홈 도전중 절약미션 요약과 대표 미션을 조회합니다.
    HomeSavingMissionSummaryRecord selectSavingMissionSummary(
            @Param("userId") Long userId,
            @Param("year") Integer year,
            @Param("month") Integer month
    );
}
