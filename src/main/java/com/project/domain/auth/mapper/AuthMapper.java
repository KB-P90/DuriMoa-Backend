package com.project.domain.auth.mapper;

import com.project.domain.auth.dto.LoginUserDTO;
import com.project.domain.auth.dto.SignupResponseDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthMapper {

    Boolean existsUserByPhone(@Param("phone") String phone);

    Integer insertUser(
            @Param("phone") String phone,
            @Param("password") String password,
            @Param("name") String name,
            @Param("role") String role);

    SignupResponseDTO findSignedUpUserByPhone(@Param("phone") String phone);

    LoginUserDTO findLoginUserByPhone(@Param("phone") String phone);

    LoginUserDTO findLoginUserById(@Param("userId") Long userId);

    Integer insertTermsAgreement(
            @Param("userId") Long userId,
            @Param("termsType") String termsType,
            @Param("agreed") Boolean agreed);
}
