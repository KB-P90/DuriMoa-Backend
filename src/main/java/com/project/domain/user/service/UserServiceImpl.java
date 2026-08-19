package com.project.domain.user.service;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.common.util.S3Uploader;
import com.project.domain.user.domain.UserProfileVO;
import com.project.domain.user.dto.UserProfileDto;
import com.project.domain.user.dto.UserProfileUpdateRequestDto;
import com.project.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

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
    @Transactional
    public UserProfileDto updateProfile(Long userId, UserProfileUpdateRequestDto request) {

        if (userId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "유저 id는 필수입니다.");
        }

        UserProfileDto existing = userMapper.get(userId);
        if (existing == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "해당 유저 정보를 찾을 수 없습니다.");
        }

        boolean removeImage = Boolean.TRUE.equals(request.getRemoveProfileImage());
        boolean hasNewImage = request.getProfileImage() != null && !request.getProfileImage().isEmpty();

        if (hasNewImage && removeImage) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "이미지 파일과 삭제 요청을 동시에 보낼 수 없습니다.");
        }

        String encodedPassword = null;
        if (StringUtils.hasText(request.getPassword())) {
            if (!StringUtils.hasText(request.getCurrentPassword())) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "현재 비밀번호를 입력해주세요.");
            }

            String existingPasswordHash = userMapper.getPasswordHash(userId);
            if (existingPasswordHash == null
                    || !passwordEncoder.matches(request.getCurrentPassword(), existingPasswordHash)) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "현재 비밀번호가 일치하지 않습니다.");
            }

            encodedPassword = passwordEncoder.encode(request.getPassword());
        }

        String normalizedPhone = null;
        if (StringUtils.hasText(request.getPhone())) {
            normalizedPhone = request.getPhone().replace("-", "");
            if (!normalizedPhone.equals(existing.getPhone())
                    && Boolean.TRUE.equals(userMapper.existsPhoneForOtherUser(normalizedPhone, userId))) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "이미 사용 중인 전화번호입니다.");
            }
        }

        String uploadedImageUrl = null;
        if (hasNewImage) {
            MultipartFile profileImage = request.getProfileImage();
            log.info("프로필 이미지 수신: originalFilename={}, contentType={}, size={} bytes",
                    profileImage.getOriginalFilename(), profileImage.getContentType(), profileImage.getSize());
            try {
                byte[] imageBytes = profileImage.getBytes();
                log.info("프로필 이미지 바이트 변환 완료: {} bytes", imageBytes.length);
                uploadedImageUrl = s3Uploader.upload(imageBytes, PROFILE_IMAGE_DIR);
                log.info("프로필 이미지 S3 업로드 완료: url={}", uploadedImageUrl);
            } catch (IOException e) {
                log.error("프로필 이미지 파일을 읽는 중 오류 발생", e);
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "이미지 파일을 읽을 수 없습니다.");
            }
        }

        UserProfileVO vo = UserProfileVO.builder()
                .id(userId)
                .name(request.getName())
                .role(request.getRole())
                .phone(normalizedPhone)
                .password(encodedPassword)
                .profileImage(uploadedImageUrl)
                .removeProfileImage(removeImage)
                .build();

        boolean result;
        try {
            result = userMapper.update(vo);
        } catch (DataIntegrityViolationException e) {
            if (uploadedImageUrl != null) {
                s3Uploader.delete(uploadedImageUrl);
            }
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "이미 사용 중인 전화번호입니다.");
        } catch (RuntimeException e) {
            if (uploadedImageUrl != null) {
                s3Uploader.delete(uploadedImageUrl);
            }
            throw e;
        }

        if (!result) {
            if (uploadedImageUrl != null) {
                s3Uploader.delete(uploadedImageUrl);
            }
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "수정을 실패했습니다.");
        }

        if (StringUtils.hasText(existing.getProfileImageUrl())
                && (removeImage
                        || (uploadedImageUrl != null && !uploadedImageUrl.equals(existing.getProfileImageUrl())))) {
            s3Uploader.delete(existing.getProfileImageUrl());
        }

        return userMapper.get(userId);
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

    @Override
    @Transactional(readOnly = true)
    public boolean isPhoneAvailable(Long userId, String phone) {
        if (userId == null || userMapper.get(userId) == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 회원입니다." + userId);
        }

        if (!StringUtils.hasText(phone)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "전화번호를 입력해주세요.");
        }

        String normalizedPhone = phone.replace("-", "");
        return !Boolean.TRUE.equals(userMapper.existsPhoneForOtherUser(normalizedPhone, userId));
    }

}
