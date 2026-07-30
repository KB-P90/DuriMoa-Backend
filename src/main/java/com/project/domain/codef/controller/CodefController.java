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

    @ApiOperation(value = "Access Token 발급", notes = "CODEF API 호출용 Access Token을 발급받습니다.")
    @PostMapping("/token")
    public ResponseEntity<ApiResponse<CodefTokenResponseDTO>> getAccessToken(
            @RequestBody(required = false) CodefTokenRequestDTO requestDTO) {
        log.info("CODEF 토큰 발급 요청 수신");
        CodefTokenResponseDTO tokenResponse = codefService.getAccessToken(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("CODEF 토큰 발급 성공", tokenResponse));
    }

    @ApiOperation(value = "Connected ID 생성", notes = "금융기관 계정 정보를 바탕으로 CODEF Connected ID를 생성합니다.")
    @PostMapping("/connected-id")
    public ResponseEntity<ApiResponse<CodefApiResponseDTO<Object>>> createConnectedId(
            @Valid @RequestBody CodefConnectedIdRequestDTO requestDTO) {
        log.info("CODEF Connected ID 생성 요청 수신");
        CodefApiResponseDTO<Object> response = codefService.createConnectedId(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("Connected ID 생성 요청 성공", response));
    }

    @ApiOperation(value = "보유 계좌 목록 조회", notes = "Connected ID를 기반으로 보유 은행 계좌 목록을 조회합니다.")
    @PostMapping("/accounts")
    public ResponseEntity<ApiResponse<CodefApiResponseDTO<Object>>> getAccountList(
            @Valid @RequestBody CodefAccountListRequestDTO requestDTO) {
        log.info("CODEF 계좌 목록 조회 요청 수신 - connectedId: {}", requestDTO.getConnectedId());
        CodefApiResponseDTO<Object> response = codefService.getAccountList(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("계좌 목록 조회 성공", response));
    }

    @ApiOperation(value = "보유 카드 목록 조회", notes = "Connected ID를 기반으로 보유 카드 목록을 조회합니다.")
    @PostMapping("/cards")
    public ResponseEntity<ApiResponse<CodefApiResponseDTO<Object>>> getCardList(
            @Valid @RequestBody CodefCardListRequestDTO requestDTO) {
        log.info("CODEF 카드 목록 조회 요청 수신 - connectedId: {}", requestDTO.getConnectedId());
        CodefApiResponseDTO<Object> response = codefService.getCardList(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("카드 목록 조회 성공", response));
    }

    @ApiOperation(value = "CODEF 범용 API 상품 요청", notes = "엔드포인트와 파라미터를 지정하여 CODEF API 상품을 요청합니다.")
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<CodefApiResponseDTO<Object>>> requestProduct(
            @Valid @RequestBody CodefProductRequestDTO requestDTO) {
        log.info("CODEF 범용 API 상품 요청 수신 - productUrl: {}", requestDTO.getProductUrl());
        CodefApiResponseDTO<Object> response = codefService.requestProduct(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("CODEF API 상품 요청 성공", response));
    }

    @ApiOperation(value = "계정 추가", notes = "기존 Connected ID에 새로운 금융기관 계정을 추가합니다.")
    @PostMapping("/connected-id/add")
    public ResponseEntity<ApiResponse<CodefApiResponseDTO<Object>>> addConnectedAccount(
            @Valid @RequestBody com.project.domain.codef.dto.CodefConnectedIdAddRequestDTO requestDTO) {
        log.info("CODEF Connected ID 계정 추가 요청 수신 - connectedId: {}", requestDTO.getConnectedId());
        CodefApiResponseDTO<Object> response = codefService.addConnectedAccount(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("계정 추가 요청 성공", response));
    }

    @ApiOperation(value = "계정 정보 수정", notes = "기존 Connected ID 내 계정 비밀번호나 아이디 정보를 수정합니다.")
    @PostMapping("/connected-id/update")
    public ResponseEntity<ApiResponse<CodefApiResponseDTO<Object>>> updateConnectedAccount(
            @Valid @RequestBody com.project.domain.codef.dto.CodefConnectedIdUpdateRequestDTO requestDTO) {
        log.info("CODEF Connected ID 계정 수정 요청 수신 - connectedId: {}", requestDTO.getConnectedId());
        CodefApiResponseDTO<Object> response = codefService.updateConnectedAccount(requestDTO);
        return ResponseEntity.ok(ApiResponse.success("계정 수정 요청 성공", response));
    }

    @ApiOperation(value = "계정 삭제", notes = "기존 Connected ID 내 특정 계정을 삭제합니다.")
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
