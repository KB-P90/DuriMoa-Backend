package com.project.domain.user.service;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.user.dto.UserProfileDto;
import com.project.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Log4j2
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

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

        if (userMapper.get(profile.getId()) == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "해당 유저 정보를 찾을 수 없습니다.");
        }

        if (StringUtils.hasText(profile.getPassword())) {
            profile.setPassword(passwordEncoder.encode(profile.getPassword()));
        }

        boolean result = userMapper.update(profile.toVo());

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


}
