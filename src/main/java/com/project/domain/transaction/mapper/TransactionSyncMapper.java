package com.project.domain.transaction.mapper;

import com.project.domain.transaction.dto.AccountTransactionRecordDTO;
import com.project.domain.transaction.dto.CardTransactionRecordDTO;
import com.project.domain.transaction.dto.TransactionSyncTargetDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TransactionSyncMapper {

    String findConnectedIdByUserId(@Param("userId") Long userId);

    List<TransactionSyncTargetDTO> findAccountsByUserId(@Param("userId") Long userId);

    List<TransactionSyncTargetDTO> findCardsByUserId(@Param("userId") Long userId);

    LocalDateTime findLastSyncAtByUserId(Long userId);

    int insertLastSync(@Param("userId") Long userId, @Param("lastSyncAt") LocalDateTime lastSyncAt);

    int updateLastSync(@Param("userId") Long userId, @Param("lastSyncAt") LocalDateTime lastSyncAt);

    int insertAccountRecord(AccountTransactionRecordDTO recordDTO);

    int insertCardRecord(CardTransactionRecordDTO recordDTO);

    List<CardTransactionRecordDTO> findNewCardRecords(@Param("userId") Long userId, @Param("lastSyncAt") LocalDateTime lastSyncAt);

    List<AccountTransactionRecordDTO> findNewAccountRecords(@Param("userId") Long userId, @Param("lastSyncAt") LocalDateTime lastSyncAt);
}
