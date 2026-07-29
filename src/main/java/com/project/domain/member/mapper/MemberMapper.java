package com.project.domain.member.mapper;

import org.apache.ibatis.annotations.Param;

public interface MemberMapper {

    boolean existsMemberById(@Param("userId") Long userId);
}
