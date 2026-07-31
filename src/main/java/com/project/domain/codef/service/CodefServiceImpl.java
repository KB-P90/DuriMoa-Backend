package com.project.domain.codef.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.config.CodefConfig;
import com.project.domain.codef.dto.*;
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
import java.util.Base64;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
@RequiredArgsConstructor
public class CodefServiceImpl implements CodefService {

    private final CodefConfig codefConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private String cachedAccessToken;
    private LocalDateTime tokenExpireTime;

    @Override
    public CodefTokenResponseDTO getAccessToken(CodefTokenRequestDTO requestDTO) {
        String clientId = (requestDTO != null && StringUtils.hasText(requestDTO.getClientId()))
                ? requestDTO.getClientId() : codefConfig.getClientId();
        String clientSecret = (requestDTO != null && StringUtils.hasText(requestDTO.getClientSecret()))
                ? requestDTO.getClientSecret() : codefConfig.getClientSecret();

        if (cachedAccessToken != null && tokenExpireTime != null && LocalDateTime.now().isBefore(tokenExpireTime)) {
            log.debug("캐시된 CODEF Access Token을 사용합니다.");
            return CodefTokenResponseDTO.builder()
                    .accessToken(cachedAccessToken)
                    .tokenType("Bearer")
                    .expiresIn(604800L)
                    .scope("read")
                    .build();
        }

        try {
            String authHeader = "Basic " + Base64.getEncoder().encodeToString(
                    (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", authHeader);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "client_credentials");
            body.add("scope", "read");

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<CodefTokenResponseDTO> response = restTemplate.postForEntity(
                    codefConfig.getOauthUrl(), entity, CodefTokenResponseDTO.class);

            if (response.getBody() == null || response.getBody().getAccessToken() == null) {
                throw new CustomException(ErrorCode.UNAUTHORIZED, "CODEF 토큰 발급 응답이 올바르지 않습니다.");
            }

            CodefTokenResponseDTO tokenDto = response.getBody();
            this.cachedAccessToken = tokenDto.getAccessToken();
            // 기본 7일 유효기간 중 안전을 위해 6.5일 후 만료 처리
            this.tokenExpireTime = LocalDateTime.now().plusSeconds(
                    tokenDto.getExpiresIn() != null ? tokenDto.getExpiresIn() - 43200L : 600000L);

            log.info("CODEF OAuth2.0 Access Token 발급 완료");
            return tokenDto;
        } catch (Exception e) {
            log.error("CODEF 토큰 발급 중 오류 발생: {}", e.getMessage());
            throw new CustomException(ErrorCode.UNAUTHORIZED, "CODEF 토큰 발급에 실패하였습니다.");
        }
    }

    @Override
    public CodefApiResponseDTO<Object> createConnectedId(CodefConnectedIdRequestDTO requestDTO) {
        encryptAccountListPasswords(requestDTO.getAccountList());
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("accountList", requestDTO.getAccountList());
        
        return executeProductRequest("/v1/account/create", codefConfig.getServiceType(), paramMap);
    }

    @Override
    public CodefApiResponseDTO<Object> getAccountList(CodefAccountListRequestDTO requestDTO) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("connectedId", requestDTO.getConnectedId());
        if (StringUtils.hasText(requestDTO.getOrganization())) {
            paramMap.put("organization", requestDTO.getOrganization());
        }
        
        return executeProductRequest("/v1/kr/bank/p/account/account-list", codefConfig.getServiceType(), paramMap);
    }

    @Override
    public CodefApiResponseDTO<Object> getCardList(CodefCardListRequestDTO requestDTO) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("connectedId", requestDTO.getConnectedId());
        if (StringUtils.hasText(requestDTO.getOrganization())) {
            paramMap.put("organization", requestDTO.getOrganization());
        }

        return executeProductRequest("/v1/kr/card/p/account/card-list", codefConfig.getServiceType(), paramMap);
    }

    @Override
    public CodefApiResponseDTO<Object> requestProduct(CodefProductRequestDTO requestDTO) {
        String serviceType = StringUtils.hasText(requestDTO.getServiceType())
                ? requestDTO.getServiceType() : codefConfig.getServiceType();
        return executeProductRequest(requestDTO.getProductUrl(), serviceType, requestDTO.getParameterMap());
    }

    @Override
    public CodefApiResponseDTO<Object> addConnectedAccount(com.project.domain.codef.dto.CodefConnectedIdAddRequestDTO requestDTO) {
        encryptAccountListPasswords(requestDTO.getAccountList());
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("connectedId", requestDTO.getConnectedId());
        paramMap.put("accountList", requestDTO.getAccountList());
        return executeProductRequest("/v1/account/add", codefConfig.getServiceType(), paramMap);
    }

    @Override
    public CodefApiResponseDTO<Object> updateConnectedAccount(com.project.domain.codef.dto.CodefConnectedIdUpdateRequestDTO requestDTO) {
        encryptAccountListPasswords(requestDTO.getAccountList());
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("connectedId", requestDTO.getConnectedId());
        paramMap.put("accountList", requestDTO.getAccountList());
        return executeProductRequest("/v1/account/update", codefConfig.getServiceType(), paramMap);
    }

    @Override
    public CodefApiResponseDTO<Object> deleteConnectedAccount(com.project.domain.codef.dto.CodefConnectedIdDeleteRequestDTO requestDTO) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("connectedId", requestDTO.getConnectedId());
        paramMap.put("accountList", requestDTO.getAccountList());
        return executeProductRequest("/v1/account/delete", codefConfig.getServiceType(), paramMap);
    }

    @Override
    public CodefApiResponseDTO<Object> getConnectedAccountList(com.project.domain.codef.dto.CodefConnectedIdAccountListRequestDTO requestDTO) {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("connectedId", requestDTO.getConnectedId());
        return executeProductRequest("/v1/account/list", codefConfig.getServiceType(), paramMap);
    }

    @Override
    public CodefApiResponseDTO<Object> getConnectedIdList() {
        return executeProductRequest("/v1/account/connectedId-list", codefConfig.getServiceType(), new HashMap<>());
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

    private CodefApiResponseDTO<Object> executeProductRequest(String productUrl, String serviceType, Map<String, Object> paramMap) {
        CodefTokenResponseDTO tokenDTO = getAccessToken(null);
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

            String requestBodyJson = objectMapper.writeValueAsString(paramMap != null ? paramMap : new HashMap<>());
            String decodedBodyJson = URLDecoder.decode(requestBodyJson, StandardCharsets.UTF_8.name());

            HttpEntity<String> entity = new HttpEntity<>(decodedBodyJson, headers);

            log.info("CODEF API 호출: [URL: {}, Environment: {}]", productUrl, serviceType);
            ResponseEntity<String> response = restTemplate.exchange(
                    targetUrl, HttpMethod.POST, entity, String.class);

            String responseBody = response.getBody();
            if (responseBody != null && (responseBody.startsWith("%") || responseBody.contains("%7B") || responseBody.contains("%22"))) {
                responseBody = URLDecoder.decode(responseBody, StandardCharsets.UTF_8.name());
            }

            Map<String, Object> rawResultMap = objectMapper.readValue(
                    responseBody != null ? responseBody : "{}", new TypeReference<Map<String, Object>>() {});

            CodefResultDTO resultDTO = null;
            if (rawResultMap.containsKey("result")) {
                resultDTO = objectMapper.convertValue(rawResultMap.get("result"), CodefResultDTO.class);
            }

            Object dataObj = rawResultMap.get("data");

            log.info("CODEF API 응답 완료: [Code: {}, Message: {}]",
                    resultDTO != null ? resultDTO.getCode() : "N/A",
                    resultDTO != null ? resultDTO.getMessage() : "N/A");

            return CodefApiResponseDTO.builder()
                    .result(resultDTO)
                    .data(dataObj)
                    .build();
        } catch (Exception e) {
            log.error("CODEF API 상품 요청 중 오류 발생: {}", e.getMessage());
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "CODEF API 요청에 실패하였습니다: " + e.getMessage());
        }
    }
}
