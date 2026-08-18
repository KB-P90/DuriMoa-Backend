package com.project.domain.user.service;

import com.project.domain.user.dto.UserProfileDto;
import com.project.domain.user.dto.UserProfileUpdateRequestDto;

public interface UserService {
    public UserProfileDto getProfile(Long no);

    public UserProfileDto updateProfile(Long userId, UserProfileUpdateRequestDto request);

    public boolean updateIsShared(Long no);

    public String getUserInvite(Long no);

    public boolean isPhoneAvailable(Long userId, String phone);
}
