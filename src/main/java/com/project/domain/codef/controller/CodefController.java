package com.project.domain.codef.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.codef.dto.CodefAccountListRequestDTO;
import com.project.domain.codef.dto.CodefApiResponseDTO;
import com.project.domain.codef.dto.CodefCardListRequestDTO;
import com.project.domain.codef.dto.CodefConnectedIdRequestDTO;
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

import com.project.domain.codef.dto.CodefBankTransactionListRequestDTO;
import com.project.domain.codef.dto.CodefBankTransactionResponseDTO;
import com.project.domain.codef.dto.CodefCardApprovalListRequestDTO;
import com.project.domain.codef.dto.CodefCardApprovalResponseDTO;
import com.project.domain.codef.dto.CodefConnectedIdResponseDTO;

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
            "- `0002`: 산업은행 | `0003`: 기업은행 | `0004`: 국민은행 | `0007`: 수협은행 | `0011`: 농협은행\n" +
            "- `0020`: 우리은행 | `0023`: SC은행 | `0027`: 씨티은행 | `0031`: 대구은행 | `0032`: 부산은행\n" +
            "- `0034`: 광주은행 | `0035`: 제주은행 | `0037`: 전북은행 | `0039`: 경남은행 | `0045`: 새마을금고\n" +
            "- `0048`: 신협은행 | `0071`: 우체국 | `0081`: KEB하나은행 | `0088`: 신한은행 | `0089`: K뱅크\n\n" +
            "**2. 주요 카드사 (businessType: CD)**\n" +
            "- `0301`: KB카드 | `0302`: 현대카드 | `0303`: 삼성카드 | `0304`: NH카드 | `0305`: BC카드\n" +
            "- `0306`: 신한카드 | `0307`: 씨티카드 | `0309`: 우리카드 | `0311`: 롯데카드 | `0313`: 하나카드\n" +
            "- `0315`: 전북카드 | `0316`: 광주카드 | `0320`: 수협카드 | `0321`: 제주카드";

    @ApiOperation(value = "Access Token 발급", notes = "CODEF API 호출용 Access Token을 발급받습니다.")
    @PostMapping("/token")
    public ResponseEntity<ApiResponse<CodefTokenResponseDTO>> getAccessToken() {
        log.info("CODEF 토큰 발급 요청 수신");
        CodefTokenResponseDTO tokenResponse = codefService.getAccessToken();
        return ResponseEntity.ok(ApiResponse.success("CODEF 토큰 발급 성공", tokenResponse));
    }

    @ApiOperation(value = "Connected ID 생성", notes = "금융기관 계정 정보를 바탕으로 CODEF Connected ID를 생성합니다." + ORG_CODE_GUIDE)
    @PostMapping("/connected-id")
    public ResponseEntity<ApiResponse<CodefApiResponseDTO<CodefConnectedIdResponseDTO>>> createConnectedId(
            @Valid @RequestBody CodefConnectedIdRequestDTO requestDTO) {
        log.info("CODEF Connected ID 생성 요청 수신");
        CodefApiResponseDTO<CodefConnectedIdResponseDTO> response = codefService.createConnectedId(requestDTO);
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

    @ApiOperation(value = "은행 거래내역 조회", notes = "Connected ID와 계좌번호를 기반으로 은행 입출금 거래내역을 조회합니다." + ORG_CODE_GUIDE)
    @PostMapping("/bank/transaction-list")
    public ResponseEntity<ApiResponse<CodefApiResponseDTO<CodefBankTransactionResponseDTO>>> getBankTransactionList(
            @Valid @RequestBody CodefBankTransactionListRequestDTO requestDTO) {
        log.info("CODEF 은행 거래내역 조회 요청 수신 - connectedId: {}, account: {}", requestDTO.getConnectedId(), requestDTO.getAccount());
        CodefApiResponseDTO<CodefBankTransactionResponseDTO> response = codefService.getBankTransactionList(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("은행 거래내역 조회 성공", response));
    }

    @ApiOperation(value = "카드 승인내역 조회", notes = "Connected ID를 기반으로 카드 승인내역을 조회합니다." + ORG_CODE_GUIDE)
    @PostMapping("/card/approval-list")
    public ResponseEntity<ApiResponse<CodefApiResponseDTO<CodefCardApprovalResponseDTO>>> getCardApprovalList(
            @Valid @RequestBody CodefCardApprovalListRequestDTO requestDTO) {
        log.info("CODEF 카드 승인내역 조회 요청 수신 - connectedId: {}", requestDTO.getConnectedId());
        CodefApiResponseDTO<CodefCardApprovalResponseDTO> response = codefService.getCardApprovalList(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("카드 승인내역 조회 성공", response));
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
