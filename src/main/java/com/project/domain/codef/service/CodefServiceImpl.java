package com.project.domain.codef.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.config.CodefConfig;
import com.project.domain.codef.dto.*;
import com.project.domain.codef.mapper.CodefTokenMapper;
import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.*;

import java.net.URLDecoder;
import java.time.LocalDateTime;

@Log4j2
@Service
@RequiredArgsConstructor
public class CodefServiceImpl implements CodefService {

    private final CodefConfig codefConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CodefTokenMapper codefTokenMapper;

    @Override
    public CodefTokenResponseDTO getAccessToken() {
        CodefTokenDTO savedToken = codefTokenMapper.selectLatestToken();

        if (savedToken != null
                && savedToken.getExpiresAt() != null
                && savedToken.getExpiresAt().isAfter(LocalDateTime.now())) {

            log.debug("DB에 저장된 유효한 CODEF Access Token을 사용합니다.");
            return CodefTokenResponseDTO.builder()
                    .accessToken(savedToken.getAccessToken())
                    .tokenType(savedToken.getTokenType())
                    .expiresIn(Duration.between(
                            LocalDateTime.now(),
                            savedToken.getExpiresAt()
                    ).getSeconds())
                    .scope("read")
                    .build();
        }

        String clientId = Optional.ofNullable(codefConfig.getClientId())
                .orElseThrow(() -> new CustomException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "CODEF 토큰 발급 필수 요청값 clientId가 null입니다."
                ));
        String clientSecret = Optional.ofNullable(codefConfig.getClientSecret())
                .orElseThrow(() -> new CustomException(
                        ErrorCode.INVALID_INPUT_VALUE,
                        "CODEF 토큰 발급 필수 요청값 clientSecret가 null입니다."
                ));

        try {
            // ----- RestTemplate Post 요청 -----
            // 클라이언트아이디, 시크릿코드 Base64 인코딩
            String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                    (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", authHeader);

            // Oauth2.0 사용자 자격증명 방식(client_credentials) 토큰 요청 설정
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("scope", "read");

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<CodefTokenResponseDTO> response = restTemplate.postForEntity(
                    codefConfig.getOauthUrl(), entity, CodefTokenResponseDTO.class);

            if (response.getBody() == null || !StringUtils.hasText(response.getBody().getAccessToken())) {
                throw new CustomException(ErrorCode.UNAUTHORIZED, "CODEF 토큰 발급 응답이 올바르지 않습니다.");
            }

            CodefTokenResponseDTO tokenDto = response.getBody();
            // 기본 7일 유효기간 중 안전을 위해 12시간 전 만료 처리 (expires_in이 604799로 반환됨)
            LocalDateTime tokenExpireTime = LocalDateTime.now()
                    .plusSeconds(tokenDto.getExpiresIn())
                    .minusHours(12);

            codefTokenMapper.upsertToken(
                    tokenDto.getAccessToken(),
                    tokenDto.getTokenType(),
                    tokenExpireTime
            );

            log.info("CODEF OAuth2.0 Access Token 발급 및 DB 저장 완료");
            return tokenDto;
        } catch (Exception e) {
            log.error("CODEF 토큰 발급 중 오류 발생: {}", e.getMessage());
            throw new CustomException(ErrorCode.UNAUTHORIZED, "CODEF 토큰 발급에 실패하였습니다.");
        }
    }

    @Override
    public String encryptPassword(String plainPassword) {
        return encryptRsaPassword(plainPassword, codefConfig.getPublicKey());
    }

    @Override
    public CodefApiResponseDTO<CodefConnectedIdResponseDTO> createConnectedId(CodefConnectedIdRequestDTO requestDTO) {
        encryptAccountListPasswords(requestDTO.getAccountList());
        return executeProductRequest("/v1/account/create", codefConfig.getServiceType(), requestDTO, CodefConnectedIdResponseDTO.class);
    }

    @Override
    public CodefApiResponseDTO<Object> getAccountList(CodefAccountListRequestDTO requestDTO) {
        return executeProductRequest("/v1/kr/bank/p/account/account-list", codefConfig.getServiceType(), requestDTO, Object.class);
    }

    @Override
    public CodefApiResponseDTO<Object> getCardList(CodefCardListRequestDTO requestDTO) {
        return executeProductRequest("/v1/kr/card/p/account/card-list", codefConfig.getServiceType(), requestDTO, Object.class);
    }

    @Override
    public CodefApiResponseDTO<CodefBankTransactionResponseDTO> getBankTransactionList(CodefBankTransactionListRequestDTO requestDTO) {
        CodefApiResponseDTO<Object> rawRes = executeProductRequest("/v1/kr/bank/p/account/transaction-list", codefConfig.getServiceType(), requestDTO, Object.class);
        CodefBankTransactionResponseDTO typedData = CodefBankTransactionResponseDTO.fromData(rawRes != null ? rawRes.getData() : null, objectMapper);

        return CodefApiResponseDTO.<CodefBankTransactionResponseDTO>builder()
                .result(rawRes != null ? rawRes.getResult() : null)
                .data(typedData)
                .build();
    }

    @Override
    public CodefApiResponseDTO<CodefCardApprovalResponseDTO> getCardApprovalList(CodefCardApprovalListRequestDTO requestDTO) {
        CodefApiResponseDTO<Object> rawRes = executeProductRequest("/v1/kr/card/p/account/approval-list", codefConfig.getServiceType(), requestDTO, Object.class);
        CodefCardApprovalResponseDTO typedData = CodefCardApprovalResponseDTO.fromData(rawRes != null ? rawRes.getData() : null, objectMapper);

        return CodefApiResponseDTO.<CodefCardApprovalResponseDTO>builder()
                .result(rawRes != null ? rawRes.getResult() : null)
                .data(typedData)
                .build();
    }

    @Override
    public CodefApiResponseDTO<Object> addConnectedAccount(com.project.domain.codef.dto.CodefConnectedIdAddRequestDTO requestDTO) {
        encryptAccountListPasswords(requestDTO.getAccountList());
        return executeProductRequest("/v1/account/add", codefConfig.getServiceType(), requestDTO, Object.class);
    }

    @Override
    public CodefApiResponseDTO<Object> updateConnectedAccount(com.project.domain.codef.dto.CodefConnectedIdUpdateRequestDTO requestDTO) {
        encryptAccountListPasswords(requestDTO.getAccountList());
        return executeProductRequest("/v1/account/update", codefConfig.getServiceType(), requestDTO, Object.class);
    }

    @Override
    public CodefApiResponseDTO<Object> deleteConnectedAccount(com.project.domain.codef.dto.CodefConnectedIdDeleteRequestDTO requestDTO) {
        return executeProductRequest("/v1/account/delete", codefConfig.getServiceType(), requestDTO, Object.class);
    }

    @Override
    public CodefApiResponseDTO<Object> getConnectedAccountList(com.project.domain.codef.dto.CodefConnectedIdAccountListRequestDTO requestDTO) {
        return executeProductRequest("/v1/account/list", codefConfig.getServiceType(), requestDTO, Object.class);
    }

    @Override
    public CodefApiResponseDTO<Object> getConnectedIdList() {
        return executeProductRequest("/v1/account/connectedId-list", codefConfig.getServiceType(), new HashMap<>(), Object.class);
    }

    private void encryptAccountListPasswords(List<CodefAccountItemDTO> accountList) {
        if (accountList == null || accountList.isEmpty()) {
            return;
        }
        String publicKeyPem = codefConfig.getPublicKey();
        for (CodefAccountItemDTO account : accountList) {
            String passwordStr = account.getPassword();
            if (StringUtils.hasText(passwordStr) && passwordStr.length() < 100) {
                log.info("계정 목록 내 평문 비밀번호 자동 RSA 암호화 수행");
                String encryptedPw = encryptRsaPassword(passwordStr, publicKeyPem);
                account.setPassword(encryptedPw);
            }
        }
    }

    private String encryptRsaPassword(String plainPassword, String publicKeyPem) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "암호화할 비밀번호가 비어 있습니다.");
        }
        if (publicKeyPem == null || publicKeyPem.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "CODEF Public Key가 지정되지 않았습니다.");
        }

        try {
            String cleanKey = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] decodedKey = Base64.getDecoder().decode(cleanKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            byte[] encryptedBytes = cipher.doFinal(plainPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("RSA 비밀번호 암호화 중 오류 발생", e);
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "비밀번호 RSA 암호화 실패: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private <T> CodefApiResponseDTO<T> executeProductRequest(String productUrl, String serviceType, Object requestPayload, Class<T> dataType) {
        CodefTokenResponseDTO tokenDTO = getAccessToken();
        String accessToken = tokenDTO.getAccessToken();

        String baseUrl;
        if ("PRODUCT".equalsIgnoreCase(serviceType)) {
            baseUrl = codefConfig.getProductUrl();
        } else {
            baseUrl = codefConfig.getDemoUrl();
        }

        String targetUrl = baseUrl + productUrl;

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + accessToken);

            String requestBodyJson = objectMapper.writeValueAsString(requestPayload != null ? requestPayload : new HashMap<>());
            String decodedBodyJson = URLDecoder.decode(requestBodyJson, StandardCharsets.UTF_8);

            HttpEntity<String> entity = new HttpEntity<>(decodedBodyJson, headers);

            log.info("CODEF API 호출: [URL: {}, Environment: {}]", productUrl, serviceType);
            ResponseEntity<String> response = restTemplate.exchange(
                    targetUrl, HttpMethod.POST, entity, String.class);

            String responseBody = response.getBody();
            if (responseBody != null && (responseBody.startsWith("%") || responseBody.contains("%7B") || responseBody.contains("%22"))) {
                responseBody = URLDecoder.decode(responseBody, StandardCharsets.UTF_8);
            }

            Map<String, Object> rawResultMap = objectMapper.readValue(
                    responseBody != null ? responseBody : "{}", new TypeReference<>() {
                    });

            CodefResultDTO resultDTO = null;
            if (rawResultMap.containsKey("result")) {
                resultDTO = objectMapper.convertValue(rawResultMap.get("result"), CodefResultDTO.class);
            }

            Object dataObj = rawResultMap.get("data");
            T typedData = null;
            if (dataObj != null) {
                if (dataType != null && !dataType.equals(Object.class)) {
                    typedData = objectMapper.convertValue(dataObj, dataType);
                } else {
                    typedData = (T) dataObj;
                }
            }

            log.info("CODEF API 응답 완료: [Code: {}, Message: {}]",
                    resultDTO != null ? resultDTO.getCode() : "N/A",
                    resultDTO != null ? resultDTO.getMessage() : "N/A");

            return CodefApiResponseDTO.<T>builder()
                    .result(resultDTO)
                    .data(typedData)
                    .build();
        } catch (Exception e) {
            log.error("CODEF API 상품 요청 중 오류 발생: {}", e.getMessage());
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "CODEF API 요청에 실패하였습니다: " + e.getMessage());
        }
    }
}
