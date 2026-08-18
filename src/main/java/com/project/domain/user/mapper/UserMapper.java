package com.project.domain.user.mapper;

import com.project.domain.user.domain.UserProfileVO;
import com.project.domain.user.dto.UserProfileDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    public UserProfileDto get(Long no);

    public boolean update(UserProfileVO profile);

    public boolean updateIsShared(long no);

    public String getUserInvite(Long no);

    public String getPasswordHash(@Param("id") Long id);

    public boolean existsPhoneForOtherUser(@Param("phone") String phone, @Param("userId") Long userId);
}
