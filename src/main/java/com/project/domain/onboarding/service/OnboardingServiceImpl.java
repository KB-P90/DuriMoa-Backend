package com.project.domain.onboarding.service;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.codef.dto.CodefAccountItemDTO;
import com.project.domain.codef.dto.CodefAccountListRequestDTO;
import com.project.domain.codef.dto.CodefApiResponseDTO;
import com.project.domain.codef.dto.CodefCardListRequestDTO;
import com.project.domain.codef.dto.CodefConnectedIdAddRequestDTO;
import com.project.domain.codef.dto.CodefConnectedIdRequestDTO;
import com.project.domain.codef.dto.CodefProductRequestDTO;
import com.project.domain.codef.service.CodefService;
import com.project.domain.onboarding.dto.OnboardingQueryRequestDTO;
import com.project.domain.onboarding.dto.OnboardingQueryResponseDTO;
import com.project.domain.onboarding.dto.OnboardingRegisterRequestDTO;
import com.project.domain.onboarding.dto.OnboardingRegisterResponseDTO;
import com.project.domain.onboarding.dto.OnboardingSelectRequestDTO;
import com.project.domain.onboarding.dto.OnboardingSelectResponseDTO;
import com.project.domain.onboarding.mapper.OnboardingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.project.common.config.CodefConfig;

@Log4j2
@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private final CodefService codefService;
    private final OnboardingMapper onboardingMapper;
    private final CodefConfig codefConfig;

    @Override
    @Transactional
    public OnboardingRegisterResponseDTO registerOrAddAccount(Long userId, OnboardingRegisterRequestDTO requestDTO) {
        CodefAccountItemDTO accountItem = requestDTO.getAccountItem();
        String businessType = accountItem.getBusinessType();
        String organization = accountItem.getOrganization();
        String companyName = resolveCompanyName(organization, businessType);

        // 1. 사용자의 기존 connected_id 조회
        String existingConnectedId = onboardingMapper.findConnectedIdByUserId(userId);
        String finalConnectedId;
        String actionType;

        if (!StringUtils.hasText(existingConnectedId)) {
            // [CASE 1] 커넥티드 ID가 없는 경우 -> CODEF 계정 등록 API 호출 (/v1/account/create)
            log.info("사용자 [{}] 커넥티드 ID 미존재 -> CODEF 계정 등록 API 호출", userId);
            CodefConnectedIdRequestDTO createReq = CodefConnectedIdRequestDTO.builder()
                    .accountList(Collections.singletonList(accountItem))
                    .build();

            CodefApiResponseDTO<Object> codefRes = codefService.createConnectedId(createReq);
            finalConnectedId = extractConnectedIdFromResponse(codefRes);
            actionType = "CREATED";

            // user 테이블에 생성된 connected_id 업데이트 저장
            onboardingMapper.updateUserConnectedId(userId, finalConnectedId);
        } else {
            // [CASE 2] 커넥티드 ID가 이미 존재하는 경우 -> CODEF 계정 추가 API 호출 (/v1/account/add)
            log.info("사용자 [{}] 기존 커넥티드 ID [{}] 존재 -> CODEF 계정 추가 API 호출", userId, existingConnectedId);
            CodefConnectedIdAddRequestDTO addReq = CodefConnectedIdAddRequestDTO.builder()
                    .connectedId(existingConnectedId)
                    .accountList(Collections.singletonList(accountItem))
                    .build();

            codefService.addConnectedAccount(addReq);
            finalConnectedId = existingConnectedId;
            actionType = "ADDED";
        }

        // 2. 계정 등록/추가 성공 시 입력받은 계정 정보(국민은행 등) 1개만 account 또는 card DB 테이블에 저장
        saveRegisteredAccountOrCard(userId, businessType, organization, companyName, accountItem.getId());

        return OnboardingRegisterResponseDTO.builder()
                .connectedId(finalConnectedId)
                .actionType(actionType)
                .businessType(businessType)
                .organization(organization)
                .company(companyName)
                .build();
    }

    private void saveRegisteredAccountOrCard(Long userId, String businessType, String organization, String companyName, String loginId) {
        String numStr = StringUtils.hasText(loginId) ? loginId : organization;
        Long parsedNum = parseNumber(numStr);

        if ("BK".equalsIgnoreCase(businessType)) {
            Long existingId = onboardingMapper.findAccountIdByUserAndNumber(userId, parsedNum, numStr, companyName);
            if (existingId == null) {
                Map<String, Object> accountParams = new HashMap<>();
                accountParams.put("userId", userId);
                accountParams.put("company", companyName);
                accountParams.put("number", parsedNum);
                accountParams.put("cardName", companyName + " 계좌");
                accountParams.put("accountId", numStr);
                accountParams.put("accountPassword", null);
                onboardingMapper.insertAccount(accountParams);
                log.info("사용자 [{}] 입력 계정 기반 account DB 저장 완료 - company: {}, accountId: {}", userId, companyName, numStr);
            }
        } else {
            Long existingId = onboardingMapper.findCardIdByUserAndNumber(userId, parsedNum, numStr, companyName);
            if (existingId == null) {
                Map<String, Object> cardParams = new HashMap<>();
                cardParams.put("userId", userId);
                cardParams.put("cardProductId", null);
                cardParams.put("company", companyName);
                cardParams.put("number", parsedNum);
                cardParams.put("cardName", companyName + " 카드");
                cardParams.put("cardId", numStr);
                cardParams.put("cardPassword", null);
                onboardingMapper.insertCard(cardParams);
                log.info("사용자 [{}] 입력 계정 기반 card DB 저장 완료 - company: {}, cardId: {}", userId, companyName, numStr);
            }
        }
    }

    @Override
    public OnboardingQueryResponseDTO getAccountsOrCards(Long userId, OnboardingQueryRequestDTO requestDTO) {
        String connectedId = requestDTO.getConnectedId();
        if (!StringUtils.hasText(connectedId)) {
            connectedId = onboardingMapper.findConnectedIdByUserId(userId);
        }
        if (!StringUtils.hasText(connectedId)) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "등록된 Connected ID가 없습니다. 먼저 계좌 또는 카드를 등록해주세요.");
        }

        Object resultData;
        if ("BK".equalsIgnoreCase(requestDTO.getBusinessType())) {
            log.info("사용자 [{}] 은행 계좌 목록 조회 요청 - connectedId: {}, org: {}", userId, connectedId, requestDTO.getOrganization());
            CodefAccountListRequestDTO req = CodefAccountListRequestDTO.builder()
                    .connectedId(connectedId)
                    .organization(requestDTO.getOrganization())
                    .build();
            CodefApiResponseDTO<Object> codefRes = codefService.getAccountList(req);
            resultData = codefRes.getData() != null ? codefRes.getData() : codefRes.getResult();
        } else {
            log.info("사용자 [{}] 보유 카드 목록 조회 요청 - connectedId: {}, org: {}", userId, connectedId, requestDTO.getOrganization());
            CodefCardListRequestDTO req = CodefCardListRequestDTO.builder()
                    .connectedId(connectedId)
                    .organization(requestDTO.getOrganization())
                    .build();
            CodefApiResponseDTO<Object> codefRes = codefService.getCardList(req);
            resultData = codefRes.getData() != null ? codefRes.getData() : codefRes.getResult();
        }

        return OnboardingQueryResponseDTO.builder()
                .connectedId(connectedId)
                .businessType(requestDTO.getBusinessType())
                .organization(requestDTO.getOrganization())
                .itemList(resultData)
                .build();
    }

    @Override
    @Transactional
    public OnboardingSelectResponseDTO selectAccountsOrCards(Long userId, OnboardingSelectRequestDTO requestDTO) {
        String connectedId = onboardingMapper.findConnectedIdByUserId(userId);
        if (!StringUtils.hasText(connectedId)) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "등록된 Connected ID가 없습니다. 먼저 계좌 또는 카드를 등록해주세요.");
        }

        String startDate = StringUtils.hasText(requestDTO.getStartDate())
                ? requestDTO.getStartDate() : LocalDate.now().minusMonths(3).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String endDate = StringUtils.hasText(requestDTO.getEndDate())
                ? requestDTO.getEndDate() : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        int totalSaved = 0;
        List<String> processedNumbers = new ArrayList<>();
        String companyName = resolveCompanyName(requestDTO.getOrganization(), requestDTO.getBusinessType());

        for (String numberStr : requestDTO.getSelectedNumbers()) {
            Long parsedNum = parseNumber(numberStr);

            if ("BK".equalsIgnoreCase(requestDTO.getBusinessType())) {
                // 기존 계좌 DB 레코드 조회 (number, account_id, 또는 company 매칭)
                Long accountId = onboardingMapper.findAccountIdByUserAndNumber(userId, parsedNum, numberStr, companyName);
                if (accountId == null) {
                    throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "선택한 계좌(" + numberStr + ") 정보가 DB에 존재하지 않습니다. 먼저 계좌를 등록해 주세요.");
                }

                // CODEF 은행 계좌 거래내역 조회
                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("connectedId", connectedId);
                paramMap.put("organization", requestDTO.getOrganization());
                paramMap.put("account", numberStr);
                paramMap.put("startDate", startDate);
                paramMap.put("endDate", endDate);
                paramMap.put("orderBy", "0");
                paramMap.put("inquireType", "1");

                CodefProductRequestDTO productReq = CodefProductRequestDTO.builder()
                        .productUrl("/v1/kr/bank/p/account/transaction-list")
                        .serviceType(codefConfig.getServiceType())
                        .parameterMap(paramMap)
                        .build();

                log.info("[CODEF-REQ] 은행 거래내역 요청 - serviceType: {}, account: {}, range: {} ~ {}",
                        codefConfig.getServiceType(), numberStr, startDate, endDate);
                CodefApiResponseDTO<Object> response = codefService.requestProduct(productReq);

                if (response != null && response.getResult() != null) {
                    log.info("[CODEF-RES] 은행 거래내역 응답 - Code: {}, Message: {}",
                            response.getResult().getCode(), response.getResult().getMessage());
                }

                int savedCount = saveAccountRecords(accountId, response != null ? response.getData() : null);
                log.info("사용자 [{}] 계좌 [{}] DB 저장 완료 건수: {}", userId, numberStr, savedCount);
                totalSaved += savedCount;
                processedNumbers.add(numberStr);
            } else {
                // 기존 카드 DB 레코드 조회 (number, card_id, 또는 company 매칭)
                Long cardId = onboardingMapper.findCardIdByUserAndNumber(userId, parsedNum, numberStr, companyName);
                if (cardId == null) {
                    throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "선택한 카드(" + numberStr + ") 정보가 DB에 존재하지 않습니다. 먼저 카드를 등록해 주세요.");
                }

                // CODEF 카드 승인 내역 조회
                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("connectedId", connectedId);
                paramMap.put("organization", requestDTO.getOrganization());
                paramMap.put("startDate", startDate);
                paramMap.put("endDate", endDate);
                paramMap.put("inquireType", "1");

                CodefProductRequestDTO productReq = CodefProductRequestDTO.builder()
                        .productUrl("/v1/kr/card/p/account/approval-list")
                        .serviceType(codefConfig.getServiceType())
                        .parameterMap(paramMap)
                        .build();

                log.info("[CODEF-REQ] 카드 승인내역 요청 - serviceType: {}, range: {} ~ {}",
                        codefConfig.getServiceType(), startDate, endDate);
                CodefApiResponseDTO<Object> response = codefService.requestProduct(productReq);

                if (response != null && response.getResult() != null) {
                    log.info("[CODEF-RES] 카드 승인내역 응답 - Code: {}, Message: {}",
                            response.getResult().getCode(), response.getResult().getMessage());
                }

                int savedCount = saveCardRecords(cardId, response != null ? response.getData() : null);
                log.info("사용자 [{}] 카드 [{}] DB 저장 완료 건수: {}", userId, numberStr, savedCount);
                totalSaved += savedCount;
                processedNumbers.add(numberStr);
            }
        }

        return OnboardingSelectResponseDTO.builder()
                .businessType(requestDTO.getBusinessType())
                .organization(requestDTO.getOrganization())
                .totalSavedRecords(totalSaved)
                .processedNumbers(processedNumbers)
                .build();
    }

    private int saveAccountRecords(Long accountId, Object dataObj) {
        if (accountId == null || dataObj == null) return 0;
        List<Map<String, Object>> itemList = extractListFromData(dataObj);
        if (itemList.isEmpty()) {
            log.info("CODEF 계좌 거래내역이 비어있습니다 (accountId: {})", accountId);
            return 0;
        }

        int saved = 0;
        for (Map<String, Object> item : itemList) {
            String trDateRaw = String.valueOf(item.getOrDefault("resAccountTrDate",
                    item.getOrDefault("resTrDate", item.getOrDefault("resUsedDate", ""))));
            String trDate = formatDate(trDateRaw);
            long outAmt = parseLong(item.get("resAccountOut"));
            long inAmt = parseLong(item.get("resAccountIn"));
            long amount = outAmt > 0 ? outAmt : (inAmt > 0 ? inAmt : parseLong(item.get("resAccountAmt")));

            String type = outAmt > 0 ? "EXPENSE" : (inAmt > 0 ? "INCOME" : "EXPENSE");
            String title = String.valueOf(item.getOrDefault("resAccountDesc1",
                    item.getOrDefault("resAccountDesc3", item.getOrDefault("resAccountDesc2", item.getOrDefault("resAccountOut", item.getOrDefault("resAccountIn", "은행 거래"))))));
            String memo = String.valueOf(item.getOrDefault("resAccountDesc2", item.getOrDefault("resAccountDesc4", "")));

            Map<String, Object> recordParams = new HashMap<>();
            recordParams.put("accountId", accountId);
            recordParams.put("type", type);
            recordParams.put("category", "개인소비");
            recordParams.put("title", title.length() > 100 ? title.substring(0, 100) : title);
            recordParams.put("amount", amount);
            recordParams.put("recordDate", trDate);
            recordParams.put("memo", memo);

            onboardingMapper.insertAccountRecord(recordParams);
            saved++;
        }
        return saved;
    }

    private int saveCardRecords(Long cardId, Object dataObj) {
        if (cardId == null || dataObj == null) return 0;
        List<Map<String, Object>> itemList = extractListFromData(dataObj);
        if (itemList.isEmpty()) {
            log.info("CODEF 카드 승인내역이 비어있습니다 (cardId: {})", cardId);
            return 0;
        }

        int saved = 0;
        for (Map<String, Object> item : itemList) {
            String trDateRaw = String.valueOf(item.getOrDefault("resUsedDate",
                    item.getOrDefault("resAccountTrDate", "")));
            String trDate = formatDate(trDateRaw);
            long amount = parseLong(item.get("resUsedAmount"));
            if (amount <= 0) {
                amount = parseLong(item.get("resApprovalAmt"));
            }
            String title = String.valueOf(item.getOrDefault("resMemberStoreName", "카드 결제"));
            String memo = String.valueOf(item.getOrDefault("resApprovalNo", ""));

            Map<String, Object> recordParams = new HashMap<>();
            recordParams.put("cardId", cardId);
            recordParams.put("type", "EXPENSE");
            recordParams.put("category", "개인소비");
            recordParams.put("title", title.length() > 100 ? title.substring(0, 100) : title);
            recordParams.put("amount", amount);
            recordParams.put("recordDate", trDate);
            recordParams.put("memo", memo);

            onboardingMapper.insertCardRecord(recordParams);
            saved++;
        }
        return saved;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractListFromData(Object dataObj) {
        if (dataObj == null) return Collections.emptyList();
        if (dataObj instanceof List) {
            return (List<Map<String, Object>>) dataObj;
        } else if (dataObj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) dataObj;
            if (map.containsKey("resTrList") && map.get("resTrList") instanceof List) {
                return (List<Map<String, Object>>) map.get("resTrList");
            }
            if (map.containsKey("resAccountTrList") && map.get("resAccountTrList") instanceof List) {
                return (List<Map<String, Object>>) map.get("resAccountTrList");
            }
            if (map.containsKey("resApprovalList") && map.get("resApprovalList") instanceof List) {
                return (List<Map<String, Object>>) map.get("resApprovalList");
            }
            if (map.containsKey("resCardApprovalList") && map.get("resCardApprovalList") instanceof List) {
                return (List<Map<String, Object>>) map.get("resCardApprovalList");
            }
            for (Object val : map.values()) {
                if (val instanceof List) {
                    return (List<Map<String, Object>>) val;
                }
            }
        }
        return Collections.emptyList();
    }

    private String extractConnectedIdFromResponse(CodefApiResponseDTO<Object> responseDTO) {
        if (responseDTO != null && responseDTO.getResult() != null) {
            String code = responseDTO.getResult().getCode();
            if (!"CF-00000".equals(code)) {
                log.error("CODEF 계정 연동 실패 - Code: {}, Message: {}", code, responseDTO.getResult().getMessage());
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE,
                        "CODEF 계정 연동 실패 [" + code + "]: " + responseDTO.getResult().getMessage());
            }
        }

        if (responseDTO != null && responseDTO.getData() != null) {
            Object data = responseDTO.getData();
            if (data instanceof Map) {
                Object cid = ((Map<?, ?>) data).get("connectedId");
                if (cid != null && StringUtils.hasText(String.valueOf(cid))) {
                    return String.valueOf(cid);
                }
            }
        }

        throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "CODEF 계정 연동 응답에서 Connected ID를 추출하지 못했습니다.");
    }

    private String formatDate(String dateRaw) {
        if (!StringUtils.hasText(dateRaw)) {
            return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
        String clean = dateRaw.replaceAll("[^0-9]", "");
        if (clean.length() == 8) {
            return clean.substring(0, 4) + "-" + clean.substring(4, 6) + "-" + clean.substring(6, 8);
        }
        return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private Long parseNumber(String val) {
        if (!StringUtils.hasText(val)) return System.currentTimeMillis();
        String clean = val.replaceAll("[^0-9]", "");
        if (clean.isEmpty()) return System.currentTimeMillis();
        try {
            return Long.parseLong(clean.length() > 18 ? clean.substring(0, 18) : clean);
        } catch (NumberFormatException e) {
            return System.currentTimeMillis();
        }
    }

    private long parseLong(Object obj) {
        if (obj == null) return 0L;
        String str = String.valueOf(obj).replaceAll("[^0-9]", "");
        if (str.isEmpty()) return 0L;
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private String resolveCompanyName(String org, String businessType) {
        if (!StringUtils.hasText(org)) return "BK".equalsIgnoreCase(businessType) ? "은행" : "카드사";
        switch (org) {
            case "0004":
                return "KB국민은행";
            case "0088":
                return "신한은행";
            case "0020":
                return "우리은행";
            case "0081":
                return "하나은행";
            case "0011":
                return "NH농협은행";
            case "0003":
                return "IBK기업은행";
            case "0090":
                return "카카오뱅크";
            case "0089":
                return "케이뱅크";
            case "0092":
                return "토스뱅크";
            case "0301":
                return "KB국민카드";
            case "0306":
                return "신한카드";
            case "0302":
                return "삼성카드";
            case "0303":
                return "현대카드";
            case "0304":
                return "롯데카드";
            case "0313":
                return "우리카드";
            case "0314":
                return "하나카드";
            case "0311":
                return "NH농협카드";
            default:
                return "BK".equalsIgnoreCase(businessType) ? "은행(" + org + ")" : "카드사(" + org + ")";
        }
    }

}
