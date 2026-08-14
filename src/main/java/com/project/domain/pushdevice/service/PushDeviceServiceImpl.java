package com.project.domain.pushdevice.service;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.pushdevice.domain.PushDeviceVO;
import com.project.domain.pushdevice.dto.PushDeviceRegisterRequestDTO;
import com.project.domain.pushdevice.dto.PushDeviceRegisterResponseDTO;
import com.project.domain.pushdevice.mapper.PushDeviceMapper;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PushDeviceServiceImpl implements PushDeviceService {

    private final PushDeviceMapper pushDeviceMapper;

    @Override
    @Transactional
    public PushDeviceRegisterResponseDTO register(Long userId, PushDeviceRegisterRequestDTO request) {
        validateUserId(userId);
        validateRegisterRequest(request);

        PushDeviceVO device = pushDeviceMapper.selectByToken(request.getToken().trim());
        if (device == null) {
            device = PushDeviceVO.builder()
                    .userId(userId)
                    .token(request.getToken().trim())
                    .platform(request.getPlatform().trim())
                    .pushEnabled(true)
                    .build();
            int insertedCount = pushDeviceMapper.insert(device);
            if (insertedCount != 1 || device.getId() == null) {
                throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "푸시 기기 등록에 실패했습니다.");
            }
            return toResponse(device);
        }

        device.setUserId(userId);
        device.setPlatform(request.getPlatform().trim());
        device.setPushEnabled(true);
        int updatedCount = pushDeviceMapper.update(device);
        if (updatedCount != 1) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "푸시 기기 등록에 실패했습니다.");
        }
        return toResponse(device);
    }

    @Override
    @Transactional
    public void unregister(Long userId, Long deviceId) {
        validateUserId(userId);
        if (deviceId == null || deviceId <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "기기 ID가 올바르지 않습니다.");
        }

        int deletedCount = pushDeviceMapper.deleteByIdAndUserId(deviceId, userId);
        if (deletedCount != 1) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "등록된 기기를 찾을 수 없습니다.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getTokens(Long userId) {
        validateUserId(userId);
        List<String> tokens = pushDeviceMapper.selectTokensByUserId(userId);
        return tokens == null ? Collections.emptyList() : tokens;
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }

    private void validateRegisterRequest(PushDeviceRegisterRequestDTO request) {
        if (request == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "푸시 기기 등록 정보가 필요합니다.");
        }
        if (request.getToken() == null || request.getToken().trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "FCM 토큰은 필수입니다.");
        }
        if (request.getPlatform() == null || request.getPlatform().trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "플랫폼은 필수입니다.");
        }
    }

    private PushDeviceRegisterResponseDTO toResponse(PushDeviceVO device) {
        return PushDeviceRegisterResponseDTO.builder()
                .deviceId(device.getId())
                .platform(device.getPlatform())
                .pushEnabled(device.getPushEnabled())
                .build();
    }
}
