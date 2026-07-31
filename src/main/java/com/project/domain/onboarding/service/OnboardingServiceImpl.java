package com.project.domain.onboarding.service;

import com.project.common.config.CodefConfig;
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
import com.project.common.enums.CodefInstitution;
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
        String inputCompany = requestDTO.getCompany();
        String loginId = requestDTO.getId();
        String loginPassword = requestDTO.getPassword();

        if (!StringUtils.hasText(loginId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "아이디(id)를 입력해 주세요.");
        }
        if (!StringUtils.hasText(loginPassword)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "비밀번호(password)를 입력해 주세요.");
        }

        CodefInstitution inst = CodefInstitution.findByCompany(inputCompany);
        String businessType = inst.getBusinessType();
        String organization = inst.getOrganizationCode();
        String companyName = inst.getOfficialName();

        CodefAccountItemDTO accountItem = CodefAccountItemDTO.builder()
                .countryCode("KR")
                .businessType(businessType)
                .clientType("P")
                .organization(organization)
                .loginType("1")
                .id(loginId)
                .password(loginPassword)
                .build();

        // 1. 사용자의 기존 connected_id 조회
        String existingConnectedId = onboardingMapper.findConnectedIdByUserId(userId);
        String finalConnectedId;
        String actionType;

        if (!StringUtils.hasText(existingConnectedId)) {
            // [CASE 1] 커넥티드 ID가 없는 경우 -> CODEF 계정 등록 API 호출 (/v1/account/create)
            log.info("사용자 [{}] 커넥티드 ID 미존재 -> CODEF 계정 등록 API 호출 ({})", userId, companyName);
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
            log.info("사용자 [{}] 기존 커넥티드 ID [{}] 존재 -> CODEF 계정 추가 API 호출 ({})", userId, existingConnectedId, companyName);
            CodefConnectedIdAddRequestDTO addReq = CodefConnectedIdAddRequestDTO.builder()
                    .connectedId(existingConnectedId)
                    .accountList(Collections.singletonList(accountItem))
                    .build();

            codefService.addConnectedAccount(addReq);
            finalConnectedId = existingConnectedId;
            actionType = "ADDED";
        }

        // 2. 계정 등록/추가 성공 시 입력받은 계정 정보 1개만 account 또는 card DB 테이블에 저장
        saveRegisteredAccountOrCard(userId, businessType, organization, companyName, loginId);

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
        String inputCompany = requestDTO.getCompany();
        CodefInstitution inst = CodefInstitution.findByCompany(inputCompany);
        String businessType = inst.getBusinessType();
        String organization = inst.getOrganizationCode();
        String companyName = inst.getOfficialName();

        String connectedId = onboardingMapper.findConnectedIdByUserId(userId);
        if (!StringUtils.hasText(connectedId)) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "등록된 Connected ID가 없습니다. 먼저 계좌 또는 카드를 등록해주세요.");
        }

        Object resultData;
        if ("BK".equalsIgnoreCase(businessType)) {
            log.info("사용자 [{}] 은행 계좌 목록 조회 요청 - company: {}, connectedId: {}, org: {}", userId, companyName, connectedId, organization);
            CodefAccountListRequestDTO req = CodefAccountListRequestDTO.builder()
                    .connectedId(connectedId)
                    .organization(organization)
                    .build();
            CodefApiResponseDTO<Object> codefRes = codefService.getAccountList(req);
            resultData = codefRes.getData() != null ? codefRes.getData() : codefRes.getResult();
        } else {
            log.info("사용자 [{}] 보유 카드 목록 조회 요청 - company: {}, connectedId: {}, org: {}", userId, companyName, connectedId, organization);
            CodefCardListRequestDTO req = CodefCardListRequestDTO.builder()
                    .connectedId(connectedId)
                    .organization(organization)
                    .build();
            CodefApiResponseDTO<Object> codefRes = codefService.getCardList(req);
            resultData = codefRes.getData() != null ? codefRes.getData() : codefRes.getResult();
        }

        return OnboardingQueryResponseDTO.builder()
                .connectedId(connectedId)
                .businessType(businessType)
                .organization(organization)
                .itemList(resultData)
                .build();
    }

    @Override
    @Transactional
    public OnboardingSelectResponseDTO selectAccountsOrCards(Long userId, OnboardingSelectRequestDTO requestDTO) {
        String inputCompany = requestDTO.getCompany();
        CodefInstitution inst = CodefInstitution.findByCompany(inputCompany);
        String businessType = inst.getBusinessType();
        String organization = inst.getOrganizationCode();
        String companyName = inst.getOfficialName();

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

        for (String numberStr : requestDTO.getSelectedNumbers()) {
            Long parsedNum = parseNumber(numberStr);

            if ("BK".equalsIgnoreCase(businessType)) {
                // 기존 계좌 DB 레코드 조회 (number, account_id, 또는 company 매칭)
                Long accountId = onboardingMapper.findAccountIdByUserAndNumber(userId, parsedNum, numberStr, companyName);
                if (accountId == null) {
                    throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "선택한 계좌(" + numberStr + ") 정보가 DB에 존재하지 않습니다. 먼저 계좌를 등록해 주세요.");
                }

                // CODEF 은행 계좌 거래내역 조회
                Map<String, Object> paramMap = new HashMap<>();
                paramMap.put("connectedId", connectedId);
                paramMap.put("organization", organization);
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
                paramMap.put("organization", organization);
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
                .businessType(businessType)
                .organization(organization)
                .totalSavedRecords(totalSaved)
                .processedNumbers(processedNumbers)
                .build();
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

    private int saveAccountRecords(Long accountId, Object dataObj) {
        if (dataObj == null) return 0;
        List<Map<String, Object>> trList = extractListFromData(dataObj);
        if (trList.isEmpty()) return 0;

        int count = 0;
        for (Map<String, Object> tr : trList) {
            String dateRaw = String.valueOf(tr.getOrDefault("resAccountTrDate", tr.getOrDefault("resTrDate", "")));
            String trDateStr = formatDate(dateRaw);

            long outAmt = parseLong(tr.get("resAccountOut"));
            long inAmt = parseLong(tr.get("resAccountIn"));

            String type;
            long amount;
            if (outAmt > 0) {
                type = "EXPENSE";
                amount = outAmt;
            } else if (inAmt > 0) {
                type = "INCOME";
                amount = inAmt;
            } else {
                long rawAmt = parseLong(tr.get("resAccountTrAmount"));
                if (rawAmt < 0) {
                    type = "EXPENSE";
                    amount = Math.abs(rawAmt);
                } else {
                    type = "INCOME";
                    amount = Math.abs(rawAmt);
                }
            }

            String desc1 = String.valueOf(tr.getOrDefault("resAccountDesc1", ""));
            String desc2 = String.valueOf(tr.getOrDefault("resAccountDesc2", ""));
            String desc3 = String.valueOf(tr.getOrDefault("resAccountDesc3", ""));

            String title = StringUtils.hasText(desc1) && !"null".equalsIgnoreCase(desc1) ? desc1 :
                    (StringUtils.hasText(desc2) && !"null".equalsIgnoreCase(desc2) ? desc2 : "계좌 거래");
            if (title.length() > 100) title = title.substring(0, 100);

            String category = "개인소비";
            String memo = StringUtils.hasText(desc3) && !"null".equalsIgnoreCase(desc3) ? desc3 : null;

            Map<String, Object> recordParams = new HashMap<>();
            recordParams.put("accountId", accountId);
            recordParams.put("type", type);
            recordParams.put("category", category);
            recordParams.put("title", title);
            recordParams.put("amount", amount);
            recordParams.put("recordDate", trDateStr);
            recordParams.put("memo", memo);

            onboardingMapper.insertAccountRecord(recordParams);
            count++;
        }
        return count;
    }

    private int saveCardRecords(Long cardId, Object dataObj) {
        if (dataObj == null) return 0;
        List<Map<String, Object>> trList = extractListFromData(dataObj);
        if (trList.isEmpty()) return 0;

        int count = 0;
        for (Map<String, Object> tr : trList) {
            String dateRaw = String.valueOf(tr.getOrDefault("resUsedDate", tr.getOrDefault("resApprovedDate", "")));
            String trDateStr = formatDate(dateRaw);

            long rawAmt = parseLong(tr.getOrDefault("resUsedAmount", tr.getOrDefault("resApprovedAmount", "0")));
            String type = rawAmt < 0 ? "INCOME" : "EXPENSE";
            long amount = Math.abs(rawAmt);

            String storeName = String.valueOf(tr.getOrDefault("resMemberStoreName", tr.getOrDefault("resStoreName", "카드 지출")));
            String title = StringUtils.hasText(storeName) && !"null".equalsIgnoreCase(storeName) ? storeName : "카드 지출";
            if (title.length() > 100) title = title.substring(0, 100);

            String category = "개인소비";

            Map<String, Object> recordParams = new HashMap<>();
            recordParams.put("cardId", cardId);
            recordParams.put("type", type);
            recordParams.put("category", category);
            recordParams.put("title", title);
            recordParams.put("amount", amount);
            recordParams.put("recordDate", trDateStr);
            recordParams.put("memo", null);

            onboardingMapper.insertCardRecord(recordParams);
            count++;
        }
        return count;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractListFromData(Object dataObj) {
        if (dataObj instanceof List) {
            return (List<Map<String, Object>>) dataObj;
        } else if (dataObj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) dataObj;
            for (Object val : map.values()) {
                if (val instanceof List) {
                    return (List<Map<String, Object>>) val;
                }
            }
        }
        return Collections.emptyList();
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
        String str = String.valueOf(obj).replaceAll("[^0-9-]", "");
        if (str.isEmpty()) return 0L;
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

}
