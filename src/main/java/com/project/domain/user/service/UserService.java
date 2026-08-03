package com.project.domain.user.service;

import com.project.domain.user.dto.UserProfileDto;

public interface UserService {
    public UserProfileDto getProfile(Long no);

    public UserProfileDto updateProfile(UserProfileDto profile);

    public boolean updateIsShared(Long no);
}
