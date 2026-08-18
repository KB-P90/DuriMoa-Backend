package com.project.domain.pushdevice.service;

import com.project.domain.pushdevice.dto.PushDeviceRegisterRequestDTO;
import com.project.domain.pushdevice.dto.PushDeviceRegisterResponseDTO;
import java.util.List;

public interface PushDeviceService {

    /**
     * 사용자의 푸시 기기를 등록합니다. 이미 등록된 토큰이면 소유자와 플랫폼을 갱신합니다.
     */
    PushDeviceRegisterResponseDTO register(Long userId, PushDeviceRegisterRequestDTO request);

    /**
     * 사용자의 푸시 기기 등록을 해제합니다.
     */
    void unregister(Long userId, Long deviceId);

    /**
     * 사용자에게 등록된 모든 기기의 FCM 토큰을 조회합니다.
     */
    List<String> getTokens(Long userId);
}
