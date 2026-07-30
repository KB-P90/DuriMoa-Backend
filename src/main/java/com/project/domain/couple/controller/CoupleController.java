package com.project.domain.couple.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.couple.dto.AcceptInvitationRequestDTO;
import com.project.domain.couple.dto.CoupleInvitationDTO;
import com.project.domain.couple.dto.InviteCodeRequestDTO;
import com.project.domain.couple.service.CoupleInvitationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.validation.Valid;
import java.util.List;

@Api(tags = "Couple", description = "커플 연결 API")
@RestController
@RequestMapping("/couple")
@RequiredArgsConstructor
public class CoupleController {

    private final CoupleInvitationService coupleInvitationService;

    @ApiOperation(value = "내 커플 연결 상태 목록 조회", notes = "동시에 여러 명에게서 요청이 와 있을 수 있어 배열로 반환한다")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<List<CoupleInvitationDTO>>> status(
            @ApiIgnore @AuthenticationPrincipal Long userId
    ) {
        List<CoupleInvitationDTO> statuses = coupleInvitationService.getStatuses(userId);
        return ResponseEntity.ok(ApiResponse.success(statuses));
    }

    @ApiOperation(value = "초대코드 입력으로 연결 요청/확정")
    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<CoupleInvitationDTO>> invite(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @Valid @RequestBody InviteCodeRequestDTO request) {
        CoupleInvitationDTO result = coupleInvitationService.invite(userId, request.getInviteCode());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @ApiOperation(value = "연결 요청 수락", notes = "나에게 와 있는 요청 중 partnerUserId로 지정한 상대의 요청을 수락한다")
    @PostMapping("/accept")
    public ResponseEntity<ApiResponse<CoupleInvitationDTO>> accept(
            @ApiIgnore @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AcceptInvitationRequestDTO request) {
        CoupleInvitationDTO result = coupleInvitationService.accept(userId, request.getPartnerUserId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @ApiOperation(value = "커플 연결 끊기", notes = "연결된 커플을 DISCONNECTED로 전환한다 (데이터는 삭제되지 않음)")
    @PostMapping("/disconnect")
    public ResponseEntity<ApiResponse<CoupleInvitationDTO>> disconnect(
            @ApiIgnore @AuthenticationPrincipal Long userId
    ) {
        CoupleInvitationDTO result = coupleInvitationService.disconnect(userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
