package com.project.domain.codef.mapper;

import com.project.domain.codef.dto.CodefTokenDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface CodefTokenMapper {

    CodefTokenDTO selectLatestToken();

    void upsertToken(
            @Param("accessToken") String accessToken,
            @Param("tokenType") String tokenType,
            @Param("expiresAt") LocalDateTime expiresAt
    );
}
