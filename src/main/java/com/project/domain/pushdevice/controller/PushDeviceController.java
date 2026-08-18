package com.project.domain.pushdevice.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.pushdevice.dto.PushDeviceRegisterRequestDTO;
import com.project.domain.pushdevice.dto.PushDeviceRegisterResponseDTO;
import com.project.domain.pushdevice.service.PushDeviceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/push-devices")
@Api(tags = "PushDevice", description = "푸시 기기 API")
public class PushDeviceController {

    private final PushDeviceService pushDeviceService;

    @PutMapping
    @ApiOperation(value = "푸시 기기 등록/갱신")
    public ResponseEntity<ApiResponse<PushDeviceRegisterResponseDTO>> register(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @Valid @RequestBody PushDeviceRegisterRequestDTO request) {

        PushDeviceRegisterResponseDTO response = pushDeviceService.register(userId, request);
        return ResponseEntity.ok(ApiResponse.success("푸시 기기가 등록되었습니다.", response));
    }

    @DeleteMapping("/{deviceId}")
    @ApiOperation(value = "푸시 기기 등록 해제")
    public ResponseEntity<ApiResponse<Void>> unregister(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @PathVariable Long deviceId) {

        pushDeviceService.unregister(userId, deviceId);
        return ResponseEntity.ok(ApiResponse.success("푸시 기기 등록이 해제되었습니다."));
    }
}
