package com.project.domain.transaction.mapper;

import com.project.domain.calendar.domain.ServiceRecord;
import com.project.domain.transaction.domain.AccountRecord;
import com.project.domain.transaction.domain.CardRecord;
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

    int insertAccountRecord(AccountRecord record);

    int insertCardRecord(CardRecord record);

    void insertServiceRecord(ServiceRecord record);

    List<CardRecord> findNewCardRecords(@Param("userId") Long userId, @Param("lastSyncAt") LocalDateTime lastSyncAt);

    List<AccountRecord> findNewAccountRecords(@Param("userId") Long userId, @Param("lastSyncAt") LocalDateTime lastSyncAt);

    AccountRecord findMatchingAccountRecord(@Param("userId") Long userId, @Param("record") CardRecord record);

    CardRecord findMatchingCardRecord(@Param("userId") Long userId, @Param("record") AccountRecord record);

    ServiceRecord findByAccountRecordId(@Param("recordId") Long recordId);

    ServiceRecord findByCardRecordId(@Param("recordId") Long recordId);

    void attachCardRecord(@Param("serviceRecordId") Long serviceRecordId, @Param("cardRecordId") Long cardRecordId);

    void attachAccountRecord(@Param("serviceRecordId") Long serviceRecordId, @Param("accountRecordId") Long accountRecordId);

}
