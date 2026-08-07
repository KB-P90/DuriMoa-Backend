package com.project.domain.user.service;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.common.util.S3Uploader;
import com.project.domain.user.dto.UserProfileDto;
import com.project.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String PROFILE_IMAGE_DIR = "profile-images";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final S3Uploader s3Uploader;

    @Override
    public UserProfileDto getProfile(Long no) {
        UserProfileDto profile = userMapper.get(no);
        if (profile == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 회원입니다." + no);
        }
        return profile;
    }

    @Override
    public UserProfileDto updateProfile(UserProfileDto profile) {

        if (profile.getId() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "유저 id는 필수입니다.");
        }

        UserProfileDto existing = userMapper.get(profile.getId());
        if (existing == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "해당 유저 정보를 찾을 수 없습니다.");
        }
        if (StringUtils.hasText(profile.getPassword())) {
            profile.setPassword(passwordEncoder.encode(profile.getPassword()));
        }

        if (profile.getProfileImage() != null && !profile.getProfileImage().isEmpty()) {
            try {
                profile.setProfileImageUrl(
                        s3Uploader.upload(profile.getProfileImage().getBytes(), PROFILE_IMAGE_DIR));
            } catch (IOException e) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "이미지 파일을 읽을 수 없습니다.");
            }
        }

        boolean result = userMapper.update(profile.toVo());

        if (result
                && profile.getProfileImageUrl() != null
                && StringUtils.hasText(existing.getProfileImageUrl())
                && !existing.getProfileImageUrl().equals(profile.getProfileImageUrl())) {
            s3Uploader.delete(existing.getProfileImageUrl());
        }

        if (!result) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "수정을 실패했습니다.");
        }
        return userMapper.get(profile.getId());
    }

    @Override
    public boolean updateIsShared(Long no) {
        UserProfileDto profile = userMapper.get(no);
        if (profile == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 회원입니다." + no);
        }

        boolean result = userMapper.updateIsShared(no);

        if (!result) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "수정을 실패했습니다.");
        }

        return result;
    }

    @Override
    public String getUserInvite(Long no) {
        UserProfileDto profile = userMapper.get(no);

        if (profile == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 회원입니다." + no);
        }

        String inviteCode = userMapper.getUserInvite(no);

        if (inviteCode == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "초대 코드를 찾을 수 없습니다." + no);
        }

        return inviteCode;
    }


}
