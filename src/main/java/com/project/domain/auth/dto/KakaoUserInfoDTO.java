package com.project.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoUserInfoDTO {

    private Long id;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    public KakaoUserInfoDTO(Long id, String name, String profileImage) {
        this.id = id;
        this.kakaoAccount = new KakaoAccount(new Profile(name, profileImage));
    }

    public String getName() {
        return kakaoAccount == null || kakaoAccount.profile == null
                ? null
                : kakaoAccount.profile.nickname;
    }

    public String getProfileImage() {
        return kakaoAccount == null || kakaoAccount.profile == null
                ? null
                : kakaoAccount.profile.profileImageUrl;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class KakaoAccount {

        @JsonProperty("profile")
        private Profile profile;

        private KakaoAccount() {
        }

        private KakaoAccount(Profile profile) {
            this.profile = profile;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Profile {

        @JsonProperty("nickname")
        private String nickname;

        @JsonProperty("profile_image_url")
        private String profileImageUrl;

        private Profile() {
        }

        private Profile(String nickname, String profileImageUrl) {
            this.nickname = nickname;
            this.profileImageUrl = profileImageUrl;
        }
    }
}
