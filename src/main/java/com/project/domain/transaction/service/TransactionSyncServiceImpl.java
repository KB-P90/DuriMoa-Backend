package com.project.domain.transaction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.enums.CodefInstitution;
import com.project.common.enums.RecordType;
import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.calendar.domain.ServiceRecord;
import com.project.domain.codef.dto.*;
import com.project.domain.codef.service.CodefService;
import com.project.domain.transaction.domain.AccountRecord;
import com.project.domain.transaction.domain.CardRecord;
import com.project.domain.transaction.dto.TransactionSyncResponseDTO;
import com.project.domain.transaction.dto.TransactionSyncTargetDTO;
import com.project.domain.transaction.mapper.TransactionSyncMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class TransactionSyncServiceImpl implements TransactionSyncService {

    // 카드, 계좌 연결 후 첫 동기화 시 받아올 기간 (3개월)
    private static final int INITIAL_LOOKBACK_MONTHS = 3;

    // 거래 내역 재동기화 간격 (5분)
    private static final int SYNC_INTERVAL_MINUTES = 5;

    private static final DateTimeFormatter CODEF_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter CODEF_TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

    private final CodefService codefService;
    private final TransactionSyncMapper transactionSyncMapper;
    private final ObjectMapper objectMapper;
    private final TransactionCategoryClassifier categoryClassifier;

    @Override
    public boolean needSync(Long userId) {
        LocalDateTime lastSyncAt = transactionSyncMapper.findLastSyncAtByUserId(userId);

        if (lastSyncAt == null) return true;

        return lastSyncAt.isBefore(LocalDateTime.now().minusMinutes(SYNC_INTERVAL_MINUTES));
    }

    @Override
    @Transactional
    public TransactionSyncResponseDTO syncUserTransactions(Long userId) {
        String connectedId = getConnectedId(userId);

        // 사용자의 모든 계좌, 카드 리스트
        List<TransactionSyncTargetDTO> accounts = transactionSyncMapper.findAccountsByUserId(userId);
        List<TransactionSyncTargetDTO> cards = transactionSyncMapper.findCardsByUserId(userId);

        LocalDate today = LocalDate.now();
        LocalDateTime previousLastSyncAt = transactionSyncMapper.findLastSyncAtByUserId(userId);
        LocalDateTime currentSyncAt = LocalDateTime.now();

        int fetchedCount = 0;
        int savedCount = 0;
        int duplicateCount = 0;

        // 모든 계좌의 입출금 내역 받아와서 저장
        for (TransactionSyncTargetDTO account : accounts) {
            TransactionSyncResponseDTO response = syncAccountTransactions(userId, connectedId, account, previousLastSyncAt, today);

            fetchedCount += response.getFetchedCount();
            savedCount += response.getSavedCount();
            duplicateCount += response.getDuplicateCount();
        }

        // 모든 카드의 승인 내역 받아와서 저장
        for (TransactionSyncTargetDTO card : cards) {
            TransactionSyncResponseDTO response = syncCardTransactions(userId, connectedId, card, previousLastSyncAt, today);

            fetchedCount += response.getFetchedCount();
            savedCount += response.getSavedCount();
            duplicateCount += response.getDuplicateCount();
        }

        // 이번 sync에서 새로 생성된 데이터를 서비스 데이터로 변환
        processNewServiceRecords(userId, previousLastSyncAt);

        // 전체 원본 거래 동기화 & 서비스 데이터 변환 모두 성공 시 checkpoint 갱신
        if (previousLastSyncAt == null) {
            transactionSyncMapper.insertLastSync(userId, currentSyncAt);
        } else {
            transactionSyncMapper.updateLastSync(userId, currentSyncAt);
        }

        log.info("사용자 거래 동기화 완료 - userId: {}, accounts: {}, cards: {}, fetched: {}, saved: {}, duplicate: {}, lastSyncAt: {}",
                userId, accounts.size(), cards.size(), fetchedCount, savedCount, duplicateCount, currentSyncAt);

        return buildResponse(fetchedCount, savedCount, duplicateCount, accounts.size(), cards.size());
    }

    // 계좌 입출금 내역 받아와서 저장
    private TransactionSyncResponseDTO syncAccountTransactions(
            Long userId,
            String connectedId,
            TransactionSyncTargetDTO account,
            LocalDateTime lastSyncAt,
            LocalDate today
    ) {
        CodefInstitution institution = validateInstitution(account.getCompany(), "BK");

        LocalDate startDate = resolveStartDate(lastSyncAt, account.isHasRecords(), today);

        String startDateString = formatCodefDate(startDate);
        String endDateString = formatCodefDate(today);

        CodefApiResponseDTO<CodefBankTransactionResponseDTO> response = codefService.getBankTransactionList(
                CodefBankTransactionListRequestDTO.builder()
                        .connectedId(connectedId)
                        .organization(institution.getOrganizationCode())
                        .account(account.getNumber())
                        .startDate(startDateString)
                        .endDate(endDateString)
                        .orderBy("1")
                        .inquiryType("1")
                        .build()
        );
        validateCodefResponse(response, "은행 거래내역");

        List<CodefBankTransactionItemDTO> items = response.getData() == null
                ? List.of() : response.getData().getItems();

        int savedCount = 0;
        int duplicateCount = 0;

        for (CodefBankTransactionItemDTO item : items) {
            AccountRecord record = toAccountRecord(userId, account.getId(), item);

            if (transactionSyncMapper.insertAccountRecord(record) == 1) {
                savedCount++;
            } else {
                duplicateCount++;
            }
        }

        log.info("CODEF 계좌 원본 거래 동기화 완료 - accountId: {}, range: {} ~ {}, fetched: {}, saved: {}, duplicate: {}",
                account.getId(), startDateString, endDateString, items.size(), savedCount, duplicateCount);

        return buildResponse(items.size(), savedCount, duplicateCount, 1, 0);
    }

    // 카드 승인 내역 받아와서 저장
    private TransactionSyncResponseDTO syncCardTransactions(
            Long userId,
            String connectedId,
            TransactionSyncTargetDTO card,
            LocalDateTime lastSyncAt,
            LocalDate today
    ) {
        CodefInstitution institution = validateInstitution(card.getCompany(), "CD");

        LocalDate startDate = resolveStartDate(lastSyncAt, card.isHasRecords(), today);

        String startDateString = formatCodefDate(startDate);
        String endDateString = formatCodefDate(today);

        CodefApiResponseDTO<CodefCardApprovalResponseDTO> response = codefService.getCardApprovalList(
                CodefCardApprovalListRequestDTO.builder()
                        .connectedId(connectedId)
                        .organization(institution.getOrganizationCode())
                        .cardNo(card.getNumber())
                        .startDate(startDateString)
                        .endDate(endDateString)
                        .orderBy("1")
                        .inquiryType("0")
                        .build()
        );
        validateCodefResponse(response, "카드 승인내역");

        List<CodefCardApprovalItemDTO> items = response.getData() == null
                ? List.of() : response.getData().getItems();

        int savedCount = 0;
        int duplicateCount = 0;

        for (CodefCardApprovalItemDTO item : items) {
            CardRecord record = toCardRecord(userId, card.getId(), item);

            if (transactionSyncMapper.insertCardRecord(record) == 1) {
                savedCount++;
            } else {
                duplicateCount++;
            }
        }

        log.info("CODEF 카드 원본 거래 동기화 완료 - cardId: {}, range: {} ~ {}, fetched: {}, saved: {}, duplicate: {}",
                card.getId(), startDateString, endDateString, items.size(), savedCount, duplicateCount);

        return buildResponse(items.size(), savedCount, duplicateCount, 0, 1);
    }

    // 카드/계좌 거래 내역을 서비스 레코드로 변환해서 저장
    private void processNewServiceRecords(Long userId, LocalDateTime lastSyncAt) {
        List<CardRecord> newCards = transactionSyncMapper.findNewCardRecords(userId, lastSyncAt);

        List<AccountRecord> newAccounts = transactionSyncMapper.findNewAccountRecords(userId, lastSyncAt);

        log.info("신규 원본 거래 서비스 변환 시작 - userId: {}, cards: {}, accounts: {}",
                userId, newCards.size(), newAccounts.size());

        for (CardRecord cardRecord : newCards) {
            processCardRecord(userId, cardRecord);
        }

        for (AccountRecord accountRecord : newAccounts) {
            processAccountRecord(userId, accountRecord);
        }
    }

    private void processCardRecord(Long userId, CardRecord cardRecord) {
        // 전체 취소(1), 거절(3)인 경우 service_record 생성 안 함
        if ("1".equals(cardRecord.getCancelStatus()) || "3".equals(cardRecord.getCancelStatus())) return;

        AccountRecord matchedAccountRecord = transactionSyncMapper.findMatchingAccountRecord(userId, cardRecord);

        log.info(
                "카드 거래 매칭 결과 - cardRecordId: {}, date: {}, time: {}, amount: {}, matchedAccountRecordIdmatchedAccountRecordId: {}",
                cardRecord.getId(),
                cardRecord.getUsedDate(),
                cardRecord.getUsedTime(),
                cardRecord.getUsedAmount(),
                matchedAccountRecord == null ? null : matchedAccountRecord.getId()
        );

        if (matchedAccountRecord != null) {
            ServiceRecord existing = transactionSyncMapper.findByAccountRecordId(matchedAccountRecord.getId());

            if (existing != null) {
                transactionSyncMapper.attachCardRecord(existing.getId(), cardRecord.getId());

                return;
            }

            transactionSyncMapper.insertServiceRecord(toServiceRecord(userId, cardRecord, matchedAccountRecord));

            return;
        }

        ServiceRecord existing = transactionSyncMapper.findByCardRecordId(cardRecord.getId());

        if (existing == null) {
            transactionSyncMapper.insertServiceRecord(toServiceRecord(userId, cardRecord, null));
        }
    }

    private void processAccountRecord(Long userId, AccountRecord accountRecord) {
        // 입금은 우선 그대로 서비스 레코드로 변환
        if (isIncome(accountRecord)) {
            if (transactionSyncMapper.findByAccountRecordId(accountRecord.getId()) == null) {
                transactionSyncMapper.insertServiceRecord(toIncomeServiceRecord(userId, accountRecord));
            }
            return;
        }

        CardRecord matchedCardRecord = transactionSyncMapper.findMatchingCardRecord(userId, accountRecord);

        log.info(
                "계좌 거래 매칭 결과 - accountRecordId: {}, date: {}, time: {}, outAmount: {}, matchedCardRecordId: {}",
                accountRecord.getId(),
                accountRecord.getTransactionDate(),
                accountRecord.getTransactionTime(),
                accountRecord.getOutAmount(),
                matchedCardRecord == null ? null : matchedCardRecord.getId()
        );

        if (matchedCardRecord != null) {
            ServiceRecord existing = transactionSyncMapper.findByCardRecordId(matchedCardRecord.getId());

            if (existing != null) {
                transactionSyncMapper.attachAccountRecord(existing.getId(), accountRecord.getId());
                return;
            }
        }

        if (transactionSyncMapper.findByAccountRecordId(accountRecord.getId()) == null) {
            transactionSyncMapper.insertServiceRecord(toExpenseServiceRecord(userId, accountRecord));
        }
    }

    private CodefInstitution validateInstitution(String company, String expectedBusinessType) {
        CodefInstitution institution = CodefInstitution.findByCompany(company);
        if (!expectedBusinessType.equals(institution.getBusinessType())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "요청한 거래 유형과 금융기관 유형이 일치하지 않습니다.");
        }
        return institution;
    }

    private String getConnectedId(Long userId) {
        String connectedId = transactionSyncMapper.findConnectedIdByUserId(userId);
        if (!StringUtils.hasText(connectedId)) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "등록된 Connected ID가 없습니다.");
        }
        return connectedId;
    }

    private void validateCodefResponse(CodefApiResponseDTO<?> response, String target) {
        if (response != null && response.isSuccess()) {
            return;
        }
        String code = response != null && response.getResult() != null ? response.getResult().getCode() : "UNKNOWN";
        String message = response != null && response.getResult() != null
                ? response.getResult().getMessage() : "CODEF 응답이 없습니다.";
        throw new CustomException(ErrorCode.INVALID_INPUT_VALUE,
                "CODEF " + target + " 조회 실패 [" + code + "]: " + message);
    }

    private AccountRecord toAccountRecord(Long userId, Long accountId, CodefBankTransactionItemDTO item) {
        LocalDate transactionDate = parseDate(item.getTransactionDate(), "은행 거래일");
        LocalTime transactionTime = parseTime(item.getTransactionTime(), "은행 거래 시각");
        Long outAmount = amountOrZero(item.getOutAmount());
        Long inAmount = amountOrZero(item.getInAmount());

        return AccountRecord.builder()
                .accountId(accountId)
                .sourceKey(createSourceKey("ACCOUNT", userId, item.getTransactionDate(), item.getTransactionTime(),
                        outAmount, inAmount, item.getAfterTranBalance(), item.getDescription1(), item.getDescription2(),
                        item.getDescription3(), item.getDescription4()))
                .transactionDate(transactionDate)
                .transactionTime(transactionTime)
                .outAmount(outAmount)
                .inAmount(inAmount)
                .afterTransactionBalance(item.getAfterTranBalance())
                .description1(item.getDescription1())
                .description2(item.getDescription2())
                .description3(item.getDescription3())
                .description4(item.getDescription4())
                .rawData(toJson(item))
                .build();
    }

    private CardRecord toCardRecord(Long userId, Long cardId, CodefCardApprovalItemDTO item) {
        LocalDate usedDate = parseDate(item.getUsedDate(), "카드 사용일");
        LocalTime usedTime = parseTime(item.getUsedTime(), "카드 사용 시각");
        LocalDate paymentDueDate = parseDateOrNull(item.getPaymentDueDate(), "카드 결제 예정일");

        long usedAmount = item.getUsedAmount() == null ? 0L : Math.abs(item.getUsedAmount());
        Long cancelAmount = item.getCancelAmount() == null ? null : Math.abs(item.getCancelAmount());

        return CardRecord.builder()
                .cardId(cardId)
                .sourceKey(createSourceKey("CARD", userId, item.getUsedDate(), item.getUsedTime(), item.getApprovalNo()))
                .usedDate(usedDate)
                .usedTime(usedTime)

                .storeName(item.getStoreName())
                .usedAmount(usedAmount)

                .paymentType(item.getPaymentType())
                .installmentMonth(item.getInstallmentMonth())

                .approvalNo(item.getApprovalNo())
                .paymentDueDate(paymentDueDate)

                .domesticForeignType(item.getDomesticForeignType())
                .storeCorpNo(item.getStoreCorpNo())
                .storeType(item.getStoreType())
                .storeNo(item.getStoreNo())

                .cancelStatus(item.getCancelYn())
                .cancelAmount(cancelAmount)

                .vat(item.getVat())
                .cashBack(item.getCashBack())

                .krwAmount(item.getKrwAmount())
                .currency(item.getCurrency())

                .rawData(toJson(item))
                .build();
    }

    private ServiceRecord toServiceRecord(Long userId, CardRecord card, AccountRecord account) {
        return ServiceRecord.builder()
                .userId(userId)
                .cardRecordId(card.getId())
                .accountRecordId(account == null ? null : account.getId())
                .recordDate(card.getUsedDate())
                .type(RecordType.EXPENSE)
                .amount(card.getUsedAmount())
                .category(categoryClassifier
                        .classifyExpense(card)
                        .name())
                .status("ACTIVE")
                .memo(card.getStoreName())
                .build();
    }

    private ServiceRecord toIncomeServiceRecord(Long userId, AccountRecord account) {
        return ServiceRecord.builder()
                .userId(userId)
                .accountRecordId(account.getId())
                .recordDate(account.getTransactionDate())
                .type(RecordType.INCOME)
                .amount(account.getInAmount())
                .category(categoryClassifier
                        .classifyExpense(account)
                        .name())
                .status("ACTIVE")
                .memo(String.join(" ",
                        account.getDescription1(),
                        account.getDescription2(),
                        account.getDescription3(),
                        account.getDescription4()))
                .build();
    }

    private ServiceRecord toExpenseServiceRecord(Long userId, AccountRecord account) {
        return ServiceRecord.builder()
                .userId(userId)
                .accountRecordId(account.getId())
                .recordDate(account.getTransactionDate())
                .type(RecordType.EXPENSE)
                .amount(account.getOutAmount())
                .category("ETC")
                .status("ACTIVE")
                .memo(account.getDescription1() + account.getDescription2() + account.getDescription3() + account.getDescription4())
                .build();
    }

    private boolean isIncome(AccountRecord accountRecord) {
        return accountRecord.getInAmount() > 0;
    }

    private Long amountOrZero(Long value) {
        return value == null ? 0L : Math.abs(value);
    }

    private LocalDate resolveStartDate(LocalDateTime lastSyncAt, boolean hasRecords, LocalDate today) {
        // 해당 카드/계좌가 최초 등록됐을 경우
        if (!hasRecords) return today.minusMonths(INITIAL_LOOKBACK_MONTHS);

        // 기존 카드/계좌
        if (lastSyncAt != null) return lastSyncAt.toLocalDate();

        return today.minusMonths(INITIAL_LOOKBACK_MONTHS);
    }

    private String formatCodefDate(LocalDate date) {
        return date.format(CODEF_DATE_FORMATTER);
    }

    private LocalDate parseDateOrNull(String value, String fieldName) {
        if (value == null || value.isBlank()) return null;

        return parseDate(value, fieldName);
    }

    private LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value, CODEF_DATE_FORMATTER);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, fieldName + "가 올바른 yyyyMMdd 형식이 아닙니다.");
        }
    }

    private LocalTime parseTime(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalTime.parse(value, CODEF_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, fieldName + "가 올바른 HHmmss 형식이 아닙니다.");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "CODEF 원본 거래 데이터 직렬화에 실패했습니다.");
        }
    }

    private String createSourceKey(Object... values) {
        StringBuilder source = new StringBuilder();

        for (Object value : values) {
            source.append(value == null ? "<null>" : value).append('|');
        }

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hex.append(String.format("%02x", value));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "거래 식별 키 생성에 실패했습니다.");
        }
    }

    private TransactionSyncResponseDTO buildResponse(
            int fetchedCount,
            int savedCount,
            int duplicateCount,
            int accountCount,
            int cardCount
    ) {
        return TransactionSyncResponseDTO.builder()
                .fetchedCount(fetchedCount)
                .savedCount(savedCount)
                .duplicateCount(duplicateCount)
                .accountCount(accountCount)
                .cardCount(cardCount)
                .build();
    }
}
