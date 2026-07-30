package com.project.domain.couple.mapper;

import com.project.domain.couple.dto.CoupleDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CoupleMapper {

    List<CoupleDTO> findCouplesByUserId(@Param("userId") Long userId);
}
