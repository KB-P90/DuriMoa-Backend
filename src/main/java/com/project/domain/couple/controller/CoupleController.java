package com.project.domain.couple.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.couple.dto.CoupleInvitationDTO;
import com.project.domain.couple.dto.InviteCodeRequestDTO;
import com.project.domain.couple.service.CoupleInvitationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Api(tags = "Couple", description = "커플 연결 API")
@RestController
@RequestMapping("/couple")
@RequiredArgsConstructor
public class CoupleController {

    private final CoupleInvitationService coupleInvitationService;

    @ApiOperation(value = "내 커플 연결 상태 조회")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<CoupleInvitationDTO>> status(
//            @AuthenticationPrincipal Long userId
    ) {
        Long userId = 3L;
        CoupleInvitationDTO status = coupleInvitationService.getStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @ApiOperation(value = "초대코드 입력으로 연결 요청/확정")
    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<CoupleInvitationDTO>> invite(
//            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody InviteCodeRequestDTO request) {
        Long userId = 3L;
        CoupleInvitationDTO result = coupleInvitationService.invite(userId, request.getInviteCode());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @ApiOperation(value = "코드 없이 커플 연결 수락", notes = "지금 나한테 와 있는 연결 요청을 코드 입력 없이 확정한다")
    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<CoupleInvitationDTO>> accept(
//            @AuthenticationPrincipal Long userId
    ) {
        Long userId = 4L;
        CoupleInvitationDTO result = coupleInvitationService.accept(userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
