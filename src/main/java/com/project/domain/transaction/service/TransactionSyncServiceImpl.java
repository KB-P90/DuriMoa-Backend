package com.project.domain.transaction.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.enums.CodefInstitution;
import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.codef.dto.CodefApiResponseDTO;
import com.project.domain.codef.dto.CodefBankTransactionItemDTO;
import com.project.domain.codef.dto.CodefBankTransactionListRequestDTO;
import com.project.domain.codef.dto.CodefBankTransactionResponseDTO;
import com.project.domain.codef.dto.CodefCardApprovalItemDTO;
import com.project.domain.codef.dto.CodefCardApprovalListRequestDTO;
import com.project.domain.codef.dto.CodefCardApprovalResponseDTO;
import com.project.domain.codef.service.CodefService;
import com.project.domain.transaction.dto.AccountTransactionRecordDTO;
import com.project.domain.transaction.dto.CardTransactionRecordDTO;
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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class TransactionSyncServiceImpl implements TransactionSyncService {

    private static final int DEFAULT_LOOKBACK_MONTHS = 1;
    private static final DateTimeFormatter CODEF_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter CODEF_TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmmss");

    private final CodefService codefService;
    private final TransactionSyncMapper transactionSyncMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public TransactionSyncResponseDTO syncUserTransactions(Long userId) {
        String connectedId = getConnectedId(userId);
        List<TransactionSyncTargetDTO> accounts = transactionSyncMapper.findAccountsByUserId(userId);
        List<TransactionSyncTargetDTO> cards = transactionSyncMapper.findCardsByUserId(userId);

        int fetchedCount = 0;
        int savedCount = 0;
        int duplicateCount = 0;
        LocalDate today = LocalDate.now();

        for (TransactionSyncTargetDTO account : accounts) {
            TransactionSyncResponseDTO response = syncAccountTransactions(connectedId, account, today);
            fetchedCount += response.getFetchedCount();
            savedCount += response.getSavedCount();
            duplicateCount += response.getDuplicateCount();
        }

        for (TransactionSyncTargetDTO card : cards) {
            TransactionSyncResponseDTO response = syncCardTransactions(connectedId, card, today);
            fetchedCount += response.getFetchedCount();
            savedCount += response.getSavedCount();
            duplicateCount += response.getDuplicateCount();
        }

        log.info("CODEF 사용자 전체 원본 거래 동기화 완료 - userId: {}, accounts: {}, cards: {}, fetched: {}, saved: {}, duplicate: {}",
                userId, accounts.size(), cards.size(), fetchedCount, savedCount, duplicateCount);

        return buildResponse(fetchedCount, savedCount, duplicateCount, accounts.size(), cards.size());
    }

    private TransactionSyncResponseDTO syncAccountTransactions(
            String connectedId,
            TransactionSyncTargetDTO account,
            LocalDate today
    ) {
        CodefInstitution institution = validateInstitution(account.getCompany(), "BK");
        String startDate = resolveStartDate(account.getLastRecordDate(), today);
        String endDate = formatCodefDate(today);

        CodefApiResponseDTO<CodefBankTransactionResponseDTO> response = codefService.getBankTransactionList(
                CodefBankTransactionListRequestDTO.builder()
                        .connectedId(connectedId)
                        .organization(institution.getOrganizationCode())
                        .account(account.getNumber())
                        .startDate(startDate)
                        .endDate(endDate)
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
            AccountTransactionRecordDTO record = toAccountRecord(account.getId(), item);
            if (transactionSyncMapper.insertAccountRecord(record) == 1) {
                savedCount++;
            } else {
                duplicateCount++;
            }
        }
        log.info("CODEF 계좌 원본 거래 동기화 완료 - accountId: {}, range: {} ~ {}, fetched: {}, saved: {}, duplicate: {}",
                account.getId(), startDate, endDate, items.size(), savedCount, duplicateCount);
        return buildResponse(items.size(), savedCount, duplicateCount, 1, 0);
    }

    private TransactionSyncResponseDTO syncCardTransactions(
            String connectedId,
            TransactionSyncTargetDTO card,
            LocalDate today
    ) {
        CodefInstitution institution = validateInstitution(card.getCompany(), "CD");
        String startDate = resolveStartDate(card.getLastRecordDate(), today);
        String endDate = formatCodefDate(today);

        CodefApiResponseDTO<CodefCardApprovalResponseDTO> response = codefService.getCardApprovalList(
                CodefCardApprovalListRequestDTO.builder()
                        .connectedId(connectedId)
                        .organization(institution.getOrganizationCode())
                        .cardNo(card.getNumber())
                        .startDate(startDate)
                        .endDate(endDate)
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
            CardTransactionRecordDTO record = toCardRecord(card.getId(), item);
            if (transactionSyncMapper.insertCardRecord(record) == 1) {
                savedCount++;
            } else {
                duplicateCount++;
            }
        }
        log.info("CODEF 카드 원본 거래 동기화 완료 - cardId: {}, range: {} ~ {}, fetched: {}, saved: {}, duplicate: {}",
                card.getId(), startDate, endDate, items.size(), savedCount, duplicateCount);
        return buildResponse(items.size(), savedCount, duplicateCount, 0, 1);
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

    private AccountTransactionRecordDTO toAccountRecord(Long accountId, CodefBankTransactionItemDTO item) {
        LocalDate transactionDate = parseDate(item.getTransactionDate(), "은행 거래일");
        LocalTime transactionTime = parseTime(item.getTransactionTime(), "은행 거래 시각");
        Long outAmount = amountOrZero(item.getOutAmount());
        Long inAmount = amountOrZero(item.getInAmount());

        return AccountTransactionRecordDTO.builder()
                .accountId(accountId)
                .sourceKey(createSourceKey("ACCOUNT", accountId, item.getTransactionDate(), item.getTransactionTime(),
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

    private CardTransactionRecordDTO toCardRecord(Long cardId, CodefCardApprovalItemDTO item) {
        LocalDate usedDate = parseDate(item.getUsedDate(), "카드 사용일");
        LocalTime usedTime = parseTime(item.getUsedTime(), "카드 사용 시각");
        LocalDate paymentDueDate = parseDateOrNull(item.getPaymentDueDate(), "카드 결제 예정일");

        long usedAmount = item.getUsedAmount() == null ? 0L : Math.abs(item.getUsedAmount());

        boolean cancelled = isCancelled(item.getCancelYn());
        Long cancelAmount = item.getCancelAmount() == null ? null : Math.abs(item.getCancelAmount());

        return CardTransactionRecordDTO.builder()
                .cardId(cardId)
                .sourceKey(createSourceKey("CARD", cardId, item.getUsedDate(), item.getUsedTime(), item.getApprovalNo()))
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

                .cancelYn(cancelled ? "Y" : "N")
                .cancelAmount(cancelled ? cancelAmount : null)

                .vat(item.getVat())
                .cashBack(item.getCashBack())

                .krwAmount(item.getKrwAmount())
                .currency(item.getCurrency())

                .rawData(toJson(item))
                .build();
    }

    private LocalDate parseDateOrNull(String value, String fieldName) {
        if (value == null || value.isBlank()) return null;

        return parseDate(value, fieldName);
    }

    private boolean isCancelled(String cancelYn) {
        return "1".equals(cancelYn) || "2".equals(cancelYn);
    }

    private LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value, CODEF_DATE_FORMATTER);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, fieldName + "가 올바른 yyyyMMdd 형식이 아닙니다.");
        }
    }

    private String resolveStartDate(LocalDate lastRecordDate, LocalDate today) {
        LocalDate startDate = lastRecordDate == null ? today.minusMonths(DEFAULT_LOOKBACK_MONTHS) : lastRecordDate;
        return formatCodefDate(startDate);
    }

    private String formatCodefDate(LocalDate date) {
        return date.format(CODEF_DATE_FORMATTER);
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

    private Long amountOrZero(Long value) {
        return value == null ? 0L : Math.abs(value);
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
