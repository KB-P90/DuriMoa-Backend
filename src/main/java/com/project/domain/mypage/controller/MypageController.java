package com.project.domain.mypage.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.mypage.dto.LinkedAssetsResponseDTO;
import com.project.domain.mypage.service.MypageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mypage")
@Api(tags = "Mypage", description = "마이페이지 API")
public class MypageController {

    private final MypageService mypageService;

    @ApiOperation(value = "연결된 계좌·카드 목록 조회")
    @GetMapping("/linked-assets")
    public ResponseEntity<ApiResponse<LinkedAssetsResponseDTO>> getLinkedAssets(
            @ApiIgnore @AuthenticationPrincipal Long userId) {

        LinkedAssetsResponseDTO linkedAssets = mypageService.getLinkedAssets(userId);
        return ResponseEntity.ok(ApiResponse.success("연결 자산 목록 조회에 성공했습니다.", linkedAssets));
    }
}
