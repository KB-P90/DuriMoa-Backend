package com.project.domain.onboarding.service;

import com.project.common.config.CodefConfig;
import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.codef.dto.CodefAccountItemDTO;
import com.project.domain.codef.dto.CodefAccountListRequestDTO;
import com.project.domain.codef.dto.CodefApiResponseDTO;
import com.project.domain.codef.dto.CodefBankTransactionItemDTO;
import com.project.domain.codef.dto.CodefBankTransactionListRequestDTO;
import com.project.domain.codef.dto.CodefBankTransactionResponseDTO;
import com.project.domain.codef.dto.CodefCardApprovalItemDTO;
import com.project.domain.codef.dto.CodefCardApprovalListRequestDTO;
import com.project.domain.codef.dto.CodefCardApprovalResponseDTO;
import com.project.domain.codef.dto.CodefCardListRequestDTO;
import com.project.domain.codef.dto.CodefConnectedIdAddRequestDTO;
import com.project.domain.codef.dto.CodefConnectedIdRequestDTO;
import com.project.domain.codef.dto.CodefConnectedIdResponseDTO;
import com.project.domain.codef.service.CodefService;
import com.project.domain.onboarding.dto.OnboardingAccountCreateRequestDTO;
import com.project.domain.onboarding.dto.OnboardingAccountRecordCreateRequestDTO;
import com.project.domain.onboarding.dto.OnboardingCardCreateRequestDTO;
import com.project.domain.onboarding.dto.OnboardingCardRecordCreateRequestDTO;
import com.project.domain.onboarding.dto.OnboardingQueryRequestDTO;
import com.project.domain.onboarding.dto.OnboardingQueryResponseDTO;
import com.project.domain.onboarding.dto.OnboardingRegisterRequestDTO;
import com.project.domain.onboarding.dto.OnboardingRegisterResponseDTO;
import com.project.domain.onboarding.dto.OnboardingSelectRequestDTO;
import com.project.domain.onboarding.dto.OnboardingSelectResponseDTO;
import com.project.domain.onboarding.dto.OnboardingSelectItemDTO;
import com.project.domain.onboarding.mapper.OnboardingMapper;
import com.project.common.enums.CodefInstitution;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Log4j2
@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private final CodefService codefService;
    private final OnboardingMapper onboardingMapper;
    private final CodefConfig codefConfig;

    @Override
    @Transactional
    public OnboardingRegisterResponseDTO registerOrAddInstitution(Long userId, OnboardingRegisterRequestDTO requestDTO) {
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
            CodefConnectedIdRequestDTO createReq = CodefConnectedIdRequestDTO.builder()
                    .accountList(Collections.singletonList(accountItem))
                    .build();

            CodefApiResponseDTO<CodefConnectedIdResponseDTO> codefRes = codefService.createConnectedId(createReq);
            if (!codefRes.isSuccess() || codefRes.getData() == null || !StringUtils.hasText(codefRes.getData().getConnectedId())) {
                String code = (codefRes.getResult() != null) ? codefRes.getResult().getCode() : "UNKNOWN";
                String msg = (codefRes.getResult() != null) ? codefRes.getResult().getMessage() : "Connected ID 추출 실패";
                log.error("CODEF 계정 연동 실패 - Code: {}, Message: {}", code, msg);
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "CODEF 계정 연동 실패 [" + code + "]: " + msg);
            }
            finalConnectedId = codefRes.getData().getConnectedId();
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

            CodefApiResponseDTO<Object> addRes = codefService.addConnectedAccount(addReq);
            if (addRes == null || !addRes.isSuccess()) {
                String code = (addRes != null && addRes.getResult() != null) ? addRes.getResult().getCode() : "UNKNOWN";
                String msg = (addRes != null && addRes.getResult() != null) ? addRes.getResult().getMessage() : "CODEF 계정 추가 실패";
                log.error("CODEF 계정 추가 실패 - Code: {}, Message: {}", code, msg);
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "CODEF 계정 추가 실패 [" + code + "]: " + msg);
            }
            finalConnectedId = existingConnectedId;
            actionType = "ADDED";
        }

        return OnboardingRegisterResponseDTO.builder()
                .connectedId(finalConnectedId)
                .actionType(actionType)
                .businessType(businessType)
                .organization(organization)
                .company(companyName)
                .build();
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
            if (!codefRes.isSuccess()) {
                String code = (codefRes.getResult() != null) ? codefRes.getResult().getCode() : "UNKNOWN";
                String msg = (codefRes.getResult() != null) ? codefRes.getResult().getMessage() : "계좌 목록 조회 실패";
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "CODEF 계좌 목록 조회 실패 [" + code + "]: " + msg);
            }
            resultData = codefRes.getData();
        } else {
            log.info("사용자 [{}] 보유 카드 목록 조회 요청 - company: {}, connectedId: {}, org: {}", userId, companyName, connectedId, organization);
            CodefCardListRequestDTO req = CodefCardListRequestDTO.builder()
                    .connectedId(connectedId)
                    .organization(organization)
                    .build();
            CodefApiResponseDTO<Object> codefRes = codefService.getCardList(req);
            if (!codefRes.isSuccess()) {
                String code = (codefRes.getResult() != null) ? codefRes.getResult().getCode() : "UNKNOWN";
                String msg = (codefRes.getResult() != null) ? codefRes.getResult().getMessage() : "카드 목록 조회 실패";
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "CODEF 카드 목록 조회 실패 [" + code + "]: " + msg);
            }
            resultData = codefRes.getData();
        }

        List<Map<String, Object>> flatItemList = extractFlatItemList(resultData);

        List<String> registeredNumbers;
        if ("BK".equalsIgnoreCase(businessType)) {
            registeredNumbers = onboardingMapper.findRegisteredAccountNumbers(userId, companyName);
        } else {
            registeredNumbers = onboardingMapper.findRegisteredCardNumbers(userId, companyName);
        }
        Set<String> registeredSet = new HashSet<>(registeredNumbers != null ? registeredNumbers : Collections.emptyList());

        for (Map<String, Object> item : flatItemList) {
            String number = (String) item.get("resAccount");
            boolean isRegistered = StringUtils.hasText(number) && registeredSet.contains(number);
            item.put("isRegistered", isRegistered);
        }

        return OnboardingQueryResponseDTO.builder()
                .itemList(flatItemList)
                .build();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractFlatItemList(Object rawData) {
        List<Map<String, Object>> flatList = new ArrayList<>();
        if (rawData == null) {
            return flatList;
        }

        if (rawData instanceof List) {
            List<?> list = (List<?>) rawData;
            for (Object item : list) {
                if (item instanceof Map) {
                    Map<String, Object> cleaned = cleanAccountOrCardItem((Map<String, Object>) item);
                    if (cleaned != null) {
                        flatList.add(cleaned);
                    }
                }
            }
        } else if (rawData instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) rawData;
            // 1) map 자체가 단일 계좌/카드 객체인 경우
            Map<String, Object> selfCleaned = cleanAccountOrCardItem(map);
            if (selfCleaned != null) {
                flatList.add(selfCleaned);
            } else {
                // 2) map 내부의 계좌/카드 리스트 또는 하위 객체 탐색
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    Object val = entry.getValue();
                    if (val instanceof List) {
                        List<?> subList = (List<?>) val;
                        for (Object item : subList) {
                            if (item instanceof Map) {
                                Map<String, Object> cleaned = cleanAccountOrCardItem((Map<String, Object>) item);
                                if (cleaned != null) {
                                    flatList.add(cleaned);
                                }
                            }
                        }
                    } else if (val instanceof Map) {
                        Map<String, Object> cleaned = cleanAccountOrCardItem((Map<String, Object>) val);
                        if (cleaned != null) {
                            flatList.add(cleaned);
                        }
                    }
                }
            }
        }
        return flatList;
    }

    private Map<String, Object> cleanAccountOrCardItem(Map<String, Object> itemMap) {
        String accountNum = getStringFromMap(itemMap, "resAccount", "resAccountNo", "resCardNo", "resDepositAccountList");

        // 계좌/카드 번호 원본이 없거나 안내/에러 문구인 항목은 스킵
        if (!StringUtils.hasText(accountNum)) {
            return null;
        }

        Map<String, Object> cleaned = new java.util.LinkedHashMap<>();
        String balance = getStringFromMap(itemMap, "resAccountBalance", "resBalance");
        String name = getStringFromMap(itemMap, "resAccountName", "resCardName", "resAccountNickName");
        String display = getStringFromMap(itemMap, "resAccountDisplay", "resCardNo");

        cleaned.put("resAccount", accountNum);
        if (StringUtils.hasText(balance)) {
            cleaned.put("resAccountBalance", balance);
        }
        if (StringUtils.hasText(name)) {
            cleaned.put("resAccountName", name);
        }
        if (StringUtils.hasText(display)) {
            cleaned.put("resAccountDisplay", display);
        }

        return cleaned;
    }

    private String getStringFromMap(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key) && map.get(key) != null) {
                String val = String.valueOf(map.get(key));
                if (StringUtils.hasText(val)) {
                    return val;
                }
            }
        }
        return null;
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

        if (requestDTO.getSelectedItems() == null || requestDTO.getSelectedItems().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "선택한 계좌/카드 목록(selectedItems)이 비어있습니다.");
        }

        for (OnboardingSelectItemDTO item : requestDTO.getSelectedItems()) {
            String numberStr = item.getNumber();
            String nameStr = item.getName();
            if (!StringUtils.hasText(numberStr)) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "선택한 계좌/카드 번호가 비어있습니다.");
            }

            if ("BK".equalsIgnoreCase(businessType)) {
                // 계좌 DB 레코드 조회 및 신규 생성
                Long accountId = onboardingMapper.findAccountIdByUserAndNumber(userId, numberStr, companyName);
                if (accountId == null) {
                    String targetAccountName = StringUtils.hasText(nameStr) ? nameStr : (companyName + " 계좌");
                    OnboardingAccountCreateRequestDTO accountReq = OnboardingAccountCreateRequestDTO.builder()
                            .userId(userId)
                            .company(companyName)
                            .number(numberStr)
                            .accountName(targetAccountName)
                            .build();
                    onboardingMapper.insertAccount(accountReq);
                    accountId = accountReq.getId();
                    log.info("사용자 [{}] 선택 계좌 account DB 신규 생성 완료 - company: {}, number: {}, name: {}, generatedId: {}",
                            userId, companyName, numberStr, targetAccountName, accountId);
                }

//                // CODEF 은행 계좌 거래내역 조회
//                CodefBankTransactionListRequestDTO bankReq = CodefBankTransactionListRequestDTO.builder()
//                        .connectedId(connectedId)
//                        .organization(organization)
//                        .account(numberStr)
//                        .startDate(startDate)
//                        .endDate(endDate)
//                        .orderBy("0")
//                        .inquiryType("1")
//                        .build();
//
//                log.info("[CODEF-REQ] 은행 거래내역 요청 - serviceType: {}, account: {}, range: {} ~ {}",
//                        codefConfig.getServiceType(), numberStr, startDate, endDate);
//                CodefApiResponseDTO<CodefBankTransactionResponseDTO> response = codefService.getBankTransactionList(bankReq);
//
//                if (response == null || !response.isSuccess()) {
//                    String code = (response != null && response.getResult() != null) ? response.getResult().getCode() : "UNKNOWN";
//                    String msg = (response != null && response.getResult() != null) ? response.getResult().getMessage() : "은행 거래내역 조회 실패";
//                    log.error("CODEF 은행 거래내역 조회 실패 - Code: {}, Message: {}", code, msg);
//                    throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "CODEF 은행 거래내역 조회 실패 [" + code + "]: " + msg);
//                }
//
//                int savedCount = saveAccountRecords(accountId, response.getData());
//                log.info("사용자 [{}] 계좌 [{}] DB 저장 완료 건수: {}", userId, numberStr, savedCount);
//                totalSaved += savedCount;
                processedNumbers.add(numberStr);
            } else {
                // 카드 DB 레코드 조회 및 신규 생성
                Long cardId = onboardingMapper.findCardIdByUserAndNumber(userId, numberStr, companyName);
                if (cardId == null) {
                    String targetCardName = StringUtils.hasText(nameStr) ? nameStr : (companyName + " 카드");
                    OnboardingCardCreateRequestDTO cardReq = OnboardingCardCreateRequestDTO.builder()
                            .userId(userId)
                            .company(companyName)
                            .number(numberStr)
                            .cardName(targetCardName)
                            .build();
                    onboardingMapper.insertCard(cardReq);
                    cardId = cardReq.getId();
                    log.info("사용자 [{}] 선택 카드 card DB 신규 생성 완료 - company: {}, number: {}, name: {}, generatedId: {}",
                            userId, companyName, numberStr, targetCardName, cardId);
                }

//                // CODEF 카드 승인 내역 조회
//                CodefCardApprovalListRequestDTO cardReq = CodefCardApprovalListRequestDTO.builder()
//                        .connectedId(connectedId)
//                        .organization(organization)
//                        .cardNo(numberStr)
//                        .startDate(startDate)
//                        .endDate(endDate)
//                        .inquiryType("1")
//                        .build();
//
//                log.info("[CODEF-REQ] 카드 승인내역 요청 - serviceType: {}, range: {} ~ {}",
//                        codefConfig.getServiceType(), startDate, endDate);
//                CodefApiResponseDTO<CodefCardApprovalResponseDTO> response = codefService.getCardApprovalList(cardReq);
//
//                if (response == null || !response.isSuccess()) {
//                    String code = (response != null && response.getResult() != null) ? response.getResult().getCode() : "UNKNOWN";
//                    String msg = (response != null && response.getResult() != null) ? response.getResult().getMessage() : "카드 승인내역 조회 실패";
//                    log.error("CODEF 카드 승인내역 조회 실패 - Code: {}, Message: {}", code, msg);
//                    throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "CODEF 카드 승인내역 조회 실패 [" + code + "]: " + msg);
//                }
//
//                int savedCount = saveCardRecords(cardId, response.getData());
//                log.info("사용자 [{}] 카드 [{}] DB 저장 완료 건수: {}", userId, numberStr, savedCount);
//                totalSaved += savedCount;
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

    private int saveAccountRecords(Long accountId, CodefBankTransactionResponseDTO responseDTO) {
        if (responseDTO == null || responseDTO.getItems() == null || responseDTO.getItems().isEmpty()) {
            return 0;
        }

        int count = 0;
        for (CodefBankTransactionItemDTO tr : responseDTO.getItems()) {
            String dateRaw = tr.getTransactionDate();
            String trDateStr = formatDate(dateRaw != null ? dateRaw : "");

            long outAmt = tr.getOutAmount() != null ? tr.getOutAmount() : 0L;
            long inAmt = tr.getInAmount() != null ? tr.getInAmount() : 0L;

            String type;
            long amount;
            if (outAmt > 0) {
                type = "EXPENSE";
                amount = outAmt;
            } else if (inAmt > 0) {
                type = "INCOME";
                amount = inAmt;
            } else {
                long rawAmt = tr.getTransactionAmount() != null ? tr.getTransactionAmount() : 0L;
                type = rawAmt < 0 ? "EXPENSE" : "INCOME";
                amount = Math.abs(rawAmt);
            }

            String desc1 = tr.getDescription1();
            String desc2 = tr.getDescription2();
            String desc3 = tr.getDescription3();

            String title = StringUtils.hasText(desc1) && !"null".equalsIgnoreCase(desc1) ? desc1 :
                    (StringUtils.hasText(desc2) && !"null".equalsIgnoreCase(desc2) ? desc2 : "계좌 거래");
            if (title.length() > 100) title = title.substring(0, 100);

            String category = "개인소비";
            String memo = StringUtils.hasText(desc3) && !"null".equalsIgnoreCase(desc3) ? desc3 : null;

            OnboardingAccountRecordCreateRequestDTO recordReq = OnboardingAccountRecordCreateRequestDTO.builder()
                    .accountId(accountId)
                    .type(type)
                    .category(category)
                    .title(title)
                    .amount(amount)
                    .recordDate(trDateStr)
                    .memo(memo)
                    .build();

            onboardingMapper.insertAccountRecord(recordReq);
            count++;
        }
        return count;
    }

    private int saveCardRecords(Long cardId, CodefCardApprovalResponseDTO responseDTO) {
        if (responseDTO == null || responseDTO.getItems() == null || responseDTO.getItems().isEmpty()) {
            return 0;
        }

        int count = 0;
        for (CodefCardApprovalItemDTO tr : responseDTO.getItems()) {
            String dateRaw = tr.getUsedDate();
            String trDateStr = formatDate(dateRaw != null ? dateRaw : "");

            long rawAmt = tr.getUsedAmount() != null ? tr.getUsedAmount() : 0L;
            String type = rawAmt < 0 ? "INCOME" : "EXPENSE";
            long amount = Math.abs(rawAmt);

            String storeName = tr.getStoreName();
            String title = StringUtils.hasText(storeName) && !"null".equalsIgnoreCase(storeName) ? storeName : "카드 지출";
            if (title.length() > 100) title = title.substring(0, 100);

            String category = "개인소비";

            OnboardingCardRecordCreateRequestDTO recordReq = OnboardingCardRecordCreateRequestDTO.builder()
                    .cardId(cardId)
                    .type(type)
                    .category(category)
                    .title(title)
                    .amount(amount)
                    .recordDate(trDateStr)
                    .memo(null)
                    .build();

            onboardingMapper.insertCardRecord(recordReq);
            count++;
        }
        return count;
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
}
