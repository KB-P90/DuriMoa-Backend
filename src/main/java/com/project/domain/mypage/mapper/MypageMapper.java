package com.project.domain.mypage.mapper;

import com.project.domain.mypage.dto.LinkedAccountDTO;
import com.project.domain.mypage.dto.LinkedCardDTO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MypageMapper {

    List<LinkedAccountDTO> selectLinkedAccounts(Long userId);

    List<LinkedCardDTO> selectLinkedCards(Long userId);
}
