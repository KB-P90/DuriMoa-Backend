package com.project.domain.member.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MemberMapper {

    boolean existsMemberById(@Param("userId") Long userId);
}
