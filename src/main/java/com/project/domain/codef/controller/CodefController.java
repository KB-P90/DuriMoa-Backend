package com.project.domain.codef.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.codef.dto.CodefAccountListRequestDTO;
import com.project.domain.codef.dto.CodefApiResponseDTO;
import com.project.domain.codef.dto.CodefCardListRequestDTO;
import com.project.domain.codef.dto.CodefConnectedIdRequestDTO;
import com.project.domain.codef.dto.CodefProductRequestDTO;
import com.project.domain.codef.dto.CodefTokenRequestDTO;
import com.project.domain.codef.dto.CodefTokenResponseDTO;
import com.project.domain.codef.service.CodefService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/codef")
@Api(tags = "CODEF API", description = "CODEF 연동 API")
public class CodefController {

    private final CodefService codefService;

    private static final String ORG_CODE_GUIDE = "\n\n" +
            "### [주요 금융기관 코드 (organization) 안내]\n" +
            "**1. 주요 은행 (businessType: BK)**\n" +
            "- `0004`: KB국민은행 | `0088`: 신한은행 | `0020`: 우리은행 | `0081`: 하나은행\n" +
            "- `0011`: NH농협은행 | `0003`: IBK기업은행 | `0090`: 카카오뱅크 | `0089`: 케이뱅크 | `0092`: 토스뱅크 | `0071`: 우체국\n\n" +
            "**2. 주요 카드사 (businessType: CD)**\n" +
            "- `0301`: KB국민카드 | `0306`: 신한카드 | `0302`: 삼성카드 | `0303`: 현대카드\n" +
            "- `0304`: 롯데카드 | `0313`: 우리카드 | `0314`: 하나카드 | `0311`: NH농협카드";

    @ApiOperation(value = "Access Token 발급", notes = "CODEF API 호출용 Access Token을 발급받습니다.")
    @PostMapping("/token")
    public ResponseEntity<ApiResponse<CodefTokenResponseDTO>> getAccessToken(
            @RequestBody(required = false) CodefTokenRequestDTO requestDTO) {
        log.info("CODEF 토큰 발급 요청 수신");
        CodefTokenResponseDTO tokenResponse = codefService.getAccessToken(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("CODEF 토큰 발급 성공", tokenResponse));
    }

    @ApiOperation(value = "Connected ID 생성", notes = "금융기관 계정 정보를 바탕으로 CODEF Connected ID를 생성합니다." + ORG_CODE_GUIDE)
    @PostMapping("/connected-id")
    public ResponseEntity<ApiResponse<CodefApiResponseDTO<Object>>> createConnectedId(
            @Valid @RequestBody CodefConnectedIdRequestDTO requestDTO) {
        log.info("CODEF Connected ID 생성 요청 수신");
        CodefApiResponseDTO<Object> response = codefService.createConnectedId(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Connected ID 생성 요청 성공", response));
    }

    @ApiOperation(value = "보유 계좌 목록 조회", notes = "Connected ID를 기반으로 보유 은행 계좌 목록을 조회합니다." + ORG_CODE_GUIDE)
    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<CodefApiResponseDTO<Object>>> getAccountList(
            @Valid @RequestBody CodefAccountListRequestDTO requestDTO) {
        log.info("CODEF 계좌 목록 조회 요청 수신 - connectedId: {}", requestDTO.getConnectedId());
        CodefApiResponseDTO<Object> response = codefService.getAccountList(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("계좌 목록 조회 성공", response));
    }

    @ApiOperation(value = "보유 카드 목록 조회", notes = "Connected ID를 기반으로 보유 카드 목록을 조회합니다." + ORG_CODE_GUIDE)
    @PostMapping("/cards")
    public ResponseEntity<ApiResponse<CodefApiResponseDTO<Object>>> getCardList(
            @Valid @RequestBody CodefCardListRequestDTO requestDTO) {
        log.info("CODEF 카드 목록 조회 요청 수신 - connectedId: {}", requestDTO.getConnectedId());
        CodefApiResponseDTO<Object> response = codefService.getCardList(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("카드 목록 조회 성공", response));
    }

    @ApiOperation(value = "CODEF 범용 API 상품 요청", notes = "엔드포인트(productUrl)와 파라미터(parameterMap)를 지정하여 CODEF API 상품을 요청합니다.\n\n" +
            "### [serviceType 기본값 안내]\n" +
            "- 기본값: `DEMO` (미입력 시 자동으로 DEMO 환경으로 요청됩니다)\n\n" +
            "### [parameterMap 필수 및 주요 파라미터 안내]\n" +
            "- **`connectedId` (필수)**: CODEF 커넥티드 아이디 (예: `\"demo_connected_id\"`)\n" +
            "- **`organization` (선택)**: 금융기관 기관 코드 (예: `\"0004\"` KB국민은행, `\"0301\"` KB국민카드)\n" +
            "- **`startDate` / `endDate` (거래내역/승인내역 조회 시 필수)**: YYYYMMDD 형태 날짜 (예: `\"20260701\"`)\n" +
            "- **`account` (특정 계좌/카드 조회 시 선택)**: 계좌번호 또는 카드번호\n\n" +
            "### [요청 예시 Body]\n" +
            "```json\n" +
            "{\n" +
            "  \"productUrl\": \"/v1/kr/bank/p/account/account-list\",\n" +
            "  \"serviceType\": \"DEMO\",\n" +
            "  \"parameterMap\": {\n" +
            "    \"connectedId\": \"demo_connected_id\",\n" +
            "    \"organization\": \"0004\"\n" +
            "  }\n" +
            "}\n" +
            "```")
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<CodefApiResponseDTO<Object>>> requestProduct(
            @Valid @RequestBody CodefProductRequestDTO requestDTO) {
        log.info("CODEF 범용 API 상품 요청 수신 - productUrl: {}", requestDTO.getProductUrl());
        CodefApiResponseDTO<Object> response = codefService.requestProduct(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("CODEF API 상품 요청 성공", response));
    }

    @ApiOperation(value = "계정 추가", notes = "기존 Connected ID에 새로운 금융기관 계정을 추가합니다." + ORG_CODE_GUIDE)
    @PostMapping("/connected-id/add")
    public ResponseEntity<ApiResponse<CodefApiResponseDTO<Object>>> addConnectedAccount(
            @Valid @RequestBody com.project.domain.codef.dto.CodefConnectedIdAddRequestDTO requestDTO) {
        log.info("CODEF Connected ID 계정 추가 요청 수신 - connectedId: {}", requestDTO.getConnectedId());
        CodefApiResponseDTO<Object> response = codefService.addConnectedAccount(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("계정 추가 요청 성공", response));
    }

    @ApiOperation(value = "계정 정보 수정", notes = "기존 Connected ID 내 계정 비밀번호나 아이디 정보를 수정합니다." + ORG_CODE_GUIDE)
    @PostMapping("/connected-id/update")
    public ResponseEntity<ApiResponse<CodefApiResponseDTO<Object>>> updateConnectedAccount(
            @Valid @RequestBody com.project.domain.codef.dto.CodefConnectedIdUpdateRequestDTO requestDTO) {
        log.info("CODEF Connected ID 계정 수정 요청 수신 - connectedId: {}", requestDTO.getConnectedId());
        CodefApiResponseDTO<Object> response = codefService.updateConnectedAccount(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("계정 수정 요청 성공", response));
    }

    @ApiOperation(value = "계정 삭제", notes = "기존 Connected ID 내 특정 계정을 삭제합니다." + ORG_CODE_GUIDE)
    @PostMapping("/connected-id/delete")
    public ResponseEntity<ApiResponse<CodefApiResponseDTO<Object>>> deleteConnectedAccount(
            @Valid @RequestBody com.project.domain.codef.dto.CodefConnectedIdDeleteRequestDTO requestDTO) {
        log.info("CODEF Connected ID 계정 삭제 요청 수신 - connectedId: {}", requestDTO.getConnectedId());
        CodefApiResponseDTO<Object> response = codefService.deleteConnectedAccount(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("계정 삭제 요청 성공", response));
    }

    @ApiOperation(value = "계정 목록 조회", notes = "특정 Connected ID 내 금융기관 계정 목록을 조회합니다.")
    @PostMapping("/connected-id/accounts")
    public ResponseEntity<ApiResponse<CodefApiResponseDTO<Object>>> getConnectedAccountList(
            @Valid @RequestBody com.project.domain.codef.dto.CodefConnectedIdAccountListRequestDTO requestDTO) {
        log.info("CODEF Connected ID 계정 목록 조회 요청 수신 - connectedId: {}", requestDTO.getConnectedId());
        CodefApiResponseDTO<Object> response = codefService.getConnectedAccountList(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Connected ID 계정 목록 조회 성공", response));
    }

    @ApiOperation(value = "Connected ID 목록 조회", notes = "생성된 전체 Connected ID 목록을 조회합니다.")
    @PostMapping("/connected-id/list")
    public ResponseEntity<ApiResponse<CodefApiResponseDTO<Object>>> getConnectedIdList() {
        log.info("CODEF 커넥티드 아이디 전체 목록 조회 요청 수신");
        CodefApiResponseDTO<Object> response = codefService.getConnectedIdList();
        return ResponseEntity.ok(ApiResponse.success("Connected ID 목록 조회 성공", response));
    }
}
