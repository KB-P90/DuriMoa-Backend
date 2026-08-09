package com.project.domain.transaction.mapper;

import com.project.domain.transaction.dto.AccountTransactionRecordDTO;
import com.project.domain.transaction.dto.CardTransactionRecordDTO;
import com.project.domain.transaction.dto.TransactionSyncTargetDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TransactionSyncMapper {

    String findConnectedIdByUserId(@Param("userId") Long userId);

    List<TransactionSyncTargetDTO> findAccountsByUserId(@Param("userId") Long userId);

    List<TransactionSyncTargetDTO> findCardsByUserId(@Param("userId") Long userId);

    int insertAccountRecord(AccountTransactionRecordDTO recordDTO);

    int insertCardRecord(CardTransactionRecordDTO recordDTO);
}
