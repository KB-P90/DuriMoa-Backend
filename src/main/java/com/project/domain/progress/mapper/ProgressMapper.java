package com.project.domain.progress.mapper;

import com.project.domain.progress.dto.ActiveGoalDTO;
import com.project.domain.progress.dto.CategoryExpenseDTO;
import com.project.domain.progress.dto.GoalItemSourceDTO;
import com.project.domain.progress.dto.MemberSourceDTO;
import com.project.domain.progress.dto.MonthlyExpenseDTO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ProgressMapper {

    ActiveGoalDTO findActiveGoal(@Param("coupleId") Long coupleId);

    List<GoalItemSourceDTO> findIncludedGoalItems(@Param("goalId") Long goalId);

    Long findActiveGoalIdByGoalItemId(
            @Param("goalItemId") Long goalItemId,
            @Param("coupleId") Long coupleId
    );

    int updateGoalItemCompletion(
            @Param("goalItemId") Long goalItemId,
            @Param("goalId") Long goalId,
            @Param("completed") Boolean completed
    );

    Integer countCompletedIncludedGoalItems(@Param("goalId") Long goalId);

    Integer countIncludedGoalItems(@Param("goalId") Long goalId);

    List<MemberSourceDTO> findMembers(
            @Param("groomId") Long groomId,
            @Param("brideId") Long brideId
    );

    Long findTotalWeddingExpense(
            @Param("userIds") List<Long> userIds,
            @Param("categories") List<String> categories
    );

    LocalDateTime findFirstWeddingExpenseDate(
            @Param("userIds") List<Long> userIds,
            @Param("categories") List<String> categories
    );

    List<CategoryExpenseDTO> findCategoryWeddingExpenses(
            @Param("userIds") List<Long> userIds,
            @Param("categories") List<String> categories
    );

    List<MonthlyExpenseDTO> findMonthlyWeddingExpenses(
            @Param("userIds") List<Long> userIds,
            @Param("categories") List<String> categories,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );
}
