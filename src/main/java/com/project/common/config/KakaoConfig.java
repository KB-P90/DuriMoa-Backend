package com.project.common.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Getter
@Configuration
@PropertySource(value = "classpath:kakao.properties", ignoreResourceNotFound = true)
public class  KakaoConfig {

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public KakaoConfig(
            @Value("${kakao.client-id:}") String clientId,
            @Value("${kakao.client-secret:}") String clientSecret,
            @Value("${kakao.redirect-uri:http://localhost:5173/auth/kakao/callback}")
                    String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }
}
