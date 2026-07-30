package com.project.domain.user.mapper;

import com.project.domain.user.domain.UserProfileVO;
import com.project.domain.user.dto.UserProfileDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    public UserProfileDto get(Long no);

    public boolean update(UserProfileVO profile);

}
