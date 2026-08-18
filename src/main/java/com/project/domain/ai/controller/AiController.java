package com.project.domain.ai.controller;

import com.project.common.response.ApiResponse;
import com.project.domain.ai.dto.AiChatRequestDTO;
import com.project.domain.ai.dto.AiChatResponseDTO;
import com.project.domain.ai.dto.AiConnectionTestResponseDTO;
import com.project.domain.ai.service.AiService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ai")
@Api(tags = "AI", description = "AI API")
public class AiController {

    private final AiService aiService;

    @PostMapping("/test")
    @ApiOperation(value = "OpenAI 연결 테스트")
    public ResponseEntity<ApiResponse<AiConnectionTestResponseDTO>> testConnection() {
        AiConnectionTestResponseDTO result = aiService.testConnection();
        return ResponseEntity.ok(ApiResponse.success("OpenAI 연결 테스트에 성공했습니다.", result));
    }

    @PostMapping("/chat")
    @ApiOperation(value = "AI 채팅 메시지 전송")
    public ResponseEntity<ApiResponse<AiChatResponseDTO>> chat(
            @Valid @RequestBody AiChatRequestDTO requestDTO
    ) {
        AiChatResponseDTO result = aiService.chat(requestDTO.getMessage());
        return ResponseEntity.ok(ApiResponse.success("AI 응답을 생성했습니다.", result));
    }
}
