package com.project.domain.auth.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KakaoUserInfoDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mapsCurrentKakaoAccountProfileResponse() throws Exception {
        String json = "{"
                + "\"id\":123456789,"
                + "\"kakao_account\":{"
                + "\"profile\":{"
                + "\"nickname\":\"카카오 사용자\","
                + "\"profile_image_url\":\"https://example.com/profile.png\""
                + "}"
                + "}"
                + "}";

        KakaoUserInfoDTO userInfo = objectMapper.readValue(json, KakaoUserInfoDTO.class);

        assertEquals(123456789L, userInfo.getId());
        assertEquals("카카오 사용자", userInfo.getName());
        assertEquals("https://example.com/profile.png", userInfo.getProfileImage());
    }
}
