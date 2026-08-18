package com.project.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class KakaoLoginResponseDTO {

    private Boolean signupRequired;
    private String signupToken;
    private String accessToken;
    private LoginUserResponseDTO user;
    private String name;
    private String profileImage;

    public static KakaoLoginResponseDTO signupRequired(
            String signupToken,
            String name,
            String profileImage) {
        return new KakaoLoginResponseDTO(
                true,
                signupToken,
                null,
                null,
                name,
                profileImage);
    }

    public static KakaoLoginResponseDTO loggedIn(
            String accessToken,
            LoginUserResponseDTO user) {
        return new KakaoLoginResponseDTO(
                false,
                null,
                accessToken,
                user,
                null,
                null);
    }
}
