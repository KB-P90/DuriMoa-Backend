package com.project.domain.auth.client;

import com.project.common.config.KakaoConfig;
import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.auth.dto.KakaoTokenResponseDTO;
import com.project.domain.auth.dto.KakaoUserInfoDTO;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class KakaoAuthClient {

    private static final Logger log = LogManager.getLogger(KakaoAuthClient.class);
    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final Pattern KAKAO_ERROR_CODE_PATTERN =
            Pattern.compile("\\\"error_code\\\"\\s*:\\s*\\\"?([A-Z0-9_-]+)\\\"?");

    private final RestTemplate restTemplate;
    private final KakaoConfig kakaoConfig;

    public KakaoUserInfoDTO getUserInfo(String authorizationCode) {
        validateConfiguration();
        String accessToken = requestAccessToken(authorizationCode);
        return requestUserInfo(accessToken);
    }

    private String requestAccessToken(String authorizationCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", kakaoConfig.getClientId());
        body.add("redirect_uri", kakaoConfig.getRedirectUri());
        body.add("code", authorizationCode);
        if (!kakaoConfig.getClientSecret().isBlank()) {
            body.add("client_secret", kakaoConfig.getClientSecret());
        }

        try {
            KakaoTokenResponseDTO response = restTemplate.postForObject(
                    TOKEN_URL,
                    new HttpEntity<>(body, headers),
                    KakaoTokenResponseDTO.class);
            if (response == null
                    || response.getAccessToken() == null
                    || response.getAccessToken().isBlank()) {
                throw new CustomException(
                        ErrorCode.UNAUTHORIZED,
                        "카카오 토큰 발급 응답이 올바르지 않습니다.");
            }
            return response.getAccessToken();
        } catch (HttpStatusCodeException exception) {
            throw kakaoApiException("토큰 발급", exception);
        } catch (RestClientException exception) {
            log.warn("카카오 토큰 발급 요청에 실패했습니다: {}", exception.getClass().getSimpleName());
            throw new CustomException(
                    ErrorCode.UNAUTHORIZED,
                    "카카오 토큰 발급에 실패했습니다.");
        }
    }

    private KakaoUserInfoDTO requestUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        String url = UriComponentsBuilder.fromHttpUrl(USER_INFO_URL)
                .queryParam("secure_resource", true)
                .encode()
                .toUriString();

        try {
            ResponseEntity<KakaoUserInfoDTO> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    KakaoUserInfoDTO.class);
            KakaoUserInfoDTO userInfo = response.getBody();
            if (userInfo == null || userInfo.getId() == null) {
                throw new CustomException(
                        ErrorCode.UNAUTHORIZED,
                        "카카오 사용자 정보 응답이 올바르지 않습니다.");
            }
            return userInfo;
        } catch (HttpStatusCodeException exception) {
            throw kakaoApiException("사용자 정보 조회", exception);
        } catch (RestClientException exception) {
            log.warn(
                    "카카오 사용자 정보 조회 요청에 실패했습니다: {}",
                    exception.getClass().getSimpleName());
            throw new CustomException(
                    ErrorCode.UNAUTHORIZED,
                    "카카오 사용자 정보 조회에 실패했습니다.");
        }
    }

    private void validateConfiguration() {
        if (kakaoConfig.getClientId().isBlank() || kakaoConfig.getRedirectUri().isBlank()) {
            throw new CustomException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "카카오 로그인 설정이 완료되지 않았습니다.");
        }
    }

    private CustomException kakaoApiException(
            String operation,
            HttpStatusCodeException exception) {
        String kakaoErrorCode = extractKakaoErrorCode(exception.getResponseBodyAsString());
        log.warn(
                "카카오 {} 실패: status={}, errorCode={}",
                operation,
                exception.getRawStatusCode(),
                kakaoErrorCode == null ? "UNKNOWN" : kakaoErrorCode);
        String errorCodeSuffix = kakaoErrorCode == null ? "" : " (" + kakaoErrorCode + ")";
        return new CustomException(
                ErrorCode.UNAUTHORIZED,
                "카카오 " + operation + "에 실패했습니다." + errorCodeSuffix);
    }

    private String extractKakaoErrorCode(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        Matcher matcher = KAKAO_ERROR_CODE_PATTERN.matcher(responseBody);
        return matcher.find() ? matcher.group(1) : null;
    }
}
