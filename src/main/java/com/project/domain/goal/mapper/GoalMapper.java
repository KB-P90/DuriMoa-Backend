package com.project.domain.goal.mapper;

import com.project.domain.goal.domain.GoalItemVO;
import com.project.domain.goal.domain.GoalVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface GoalMapper {

    Integer insertGoal(GoalVO goal);

    Integer insertGoalItems(
            @Param("goalId") Long goalId,
            @Param("items") List<GoalItemVO> items
    );
}
