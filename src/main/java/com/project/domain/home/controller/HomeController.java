package com.project.domain.home.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.home.dto.HomeResponseDTO;
import com.project.domain.home.exception.HomeException.HomeAuthenticationRequiredException;
import com.project.domain.home.service.HomeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "Home", description = "홈 화면 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/home")
public class HomeController {

    // 홈 화면 조회 응답을 구성하는 서비스입니다.
    private final HomeService homeService;

    // 인증 사용자의 홈 화면 데이터와 준비 체크리스트를 조회합니다.
    @ApiOperation(value = "홈 조회", notes = "인증 사용자 기준으로 홈 화면 뼈대와 5단계 준비 체크리스트를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<HomeResponseDTO>> getHome(
            @AuthenticationPrincipal Long userId,
            Authentication authentication
    ) {
        Long authenticatedUserId = resolveAuthenticatedUserId(userId, authentication);
        HomeResponseDTO response = homeService.getHome(authenticatedUserId);
        return ResponseEntity.ok(ApiResponse.success("홈 화면 조회에 성공했습니다.", response));
    }

    // 인증 principal 또는 Authentication에서 사용자 ID가 유효한지 확인합니다.
    private Long resolveAuthenticatedUserId(Long userId, Authentication authentication) {
        if (userId == null || userId <= 0) {
            Long authenticationUserId = extractUserIdFromAuthentication(authentication);
            if (authenticationUserId == null) {
                throw new HomeAuthenticationRequiredException();
            }
            return authenticationUserId;
        }
        return userId;
    }

    // XML MVC 환경에서 principal 주입이 비어 있을 때 Authentication에서 사용자 ID를 꺼냅니다.
    private Long extractUserIdFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Number) {
            Long authenticationUserId = ((Number) principal).longValue();
            return authenticationUserId > 0 ? authenticationUserId : null;
        }
        return null;
    }
}
