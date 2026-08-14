package com.project.domain.pushdevice.mapper;

import com.project.domain.pushdevice.domain.PushDeviceVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PushDeviceMapper {

    PushDeviceVO selectByToken(String token);

    int insert(PushDeviceVO device);

    int update(PushDeviceVO device);

    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    List<String> selectTokensByUserId(Long userId);
}
