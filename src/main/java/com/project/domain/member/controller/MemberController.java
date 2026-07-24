package com.project.domain.member.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@Api(tags = "Member", description = "회원 관리 API (Swagger 연동 테스트용)")
@RestController
@RequestMapping("/api/members")
public class MemberController {

    @ApiOperation(value = "Swagger 연결 테스트 API", notes = "Swagger UI 연결 및 실행 상태를 확인하는 테스트 API입니다.")
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testSwagger() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", 200);
        response.put("message", "KB 두리모아 백엔드 Swagger 연동 성공!");
        return ResponseEntity.ok(response);
    }
}
