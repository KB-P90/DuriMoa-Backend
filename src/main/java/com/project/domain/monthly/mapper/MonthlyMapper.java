package com.project.domain.monthly.mapper;

import com.project.domain.monthly.dto.ExpenseCategoryDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MonthlyMapper {

    List<ExpenseCategoryDTO> selectMonthlyExpenseCategories(@Param("year") Integer year, @Param("month") Integer month);
}
