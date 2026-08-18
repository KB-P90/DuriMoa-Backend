package com.project.domain.ai.mapper;

import com.project.domain.ai.model.AiAnalysisMemberRecord;
import com.project.domain.ai.model.AiMonthlyFinanceRecord;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiRecommendationMapper {

    List<AiAnalysisMemberRecord> selectAnalysisMembers(@Param("userId") Long userId);

    List<AiMonthlyFinanceRecord> selectMonthlyFinances(
            @Param("userIds") List<Long> userIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
