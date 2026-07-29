package com.project.domain.auth.mapper;

import com.project.domain.auth.dto.SignupResponseDTO;
import com.project.domain.auth.dto.LoginUserDTO;
import org.apache.ibatis.annotations.Param;

public interface AuthMapper {

    Boolean existsUserByPhone(@Param("phone") String phone);

    Integer insertUser(
            @Param("phone") String phone,
            @Param("password") String password,
            @Param("name") String name,
            @Param("role") String role);

    SignupResponseDTO findSignedUpUserByPhone(@Param("phone") String phone);

    LoginUserDTO findLoginUserByPhone(@Param("phone") String phone);
}
