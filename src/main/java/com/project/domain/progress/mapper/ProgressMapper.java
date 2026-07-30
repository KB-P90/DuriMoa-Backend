package com.project.domain.progress.mapper;

import com.project.domain.progress.dto.CoupleSavingSummaryDTO;
import com.project.domain.progress.dto.MonthlySavingDTO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProgressMapper {

    boolean existsSavingRecord(@Param("groomId") Long groomId, @Param("brideId") Long brideId);

    Long findTotalSavingAmount(@Param("groomId") Long groomId, @Param("brideId") Long brideId);

    CoupleSavingSummaryDTO findCoupleSavingSummary(
            @Param("groomId") Long groomId,
            @Param("brideId") Long brideId
    );

    List<MonthlySavingDTO> findMonthlySavingAmounts(
            @Param("groomId") Long groomId,
            @Param("brideId") Long brideId
    );
}
