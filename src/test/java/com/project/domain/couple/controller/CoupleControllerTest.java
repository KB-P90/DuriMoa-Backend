package com.project.domain.couple.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.couple.dto.CoupleInvitationDTO;
import com.project.domain.couple.service.CoupleInvitationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 컨트롤러가 아직 @AuthenticationPrincipal 대신 userId를 하드코딩(3L/4L)해 둔 테스트 단계라
// 여기 테스트도 그 값 기준으로 맞춰뒀다. 실제 인증 붙이면 같이 정리 필요.
@ExtendWith(MockitoExtension.class)
class CoupleControllerTest {

    @Mock
    private CoupleInvitationService coupleInvitationService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        CoupleController coupleController = new CoupleController(coupleInvitationService);
        mockMvc = MockMvcBuilders.standaloneSetup(coupleController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void statusReturnsMyCoupleStatus() throws Exception {
        when(coupleInvitationService.getStatus(3L))
                .thenReturn(new CoupleInvitationDTO(2L, "이유진", "B", "REQUESTED"));

        MvcResult result = mockMvc.perform(get("/couple/status"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = readResponse(result);

        assertTrue(response.get("success").asBoolean());
        assertEquals("REQUESTED", response.at("/data/status").asText());
        assertEquals("이유진", response.at("/data/name").asText());
    }

    @Test
    void inviteReturnsConnectionResult() throws Exception {
        when(coupleInvitationService.invite(eq(3L), eq("XYZ789")))
                .thenReturn(new CoupleInvitationDTO(2L, "이유진", "B", "WAIT"));

        MvcResult result = mockMvc.perform(post("/couple/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inviteCode\":\"XYZ789\"}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = readResponse(result);

        assertTrue(response.get("success").asBoolean());
        assertEquals(2L, response.at("/data/userId").asLong());
        assertEquals("이유진", response.at("/data/name").asText());
        assertEquals("WAIT", response.at("/data/status").asText());
    }

    @Test
    void acceptConfirmsPendingRequestWithoutBody() throws Exception {
        when(coupleInvitationService.accept(4L))
                .thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", "CONNECTED"));

        MvcResult result = mockMvc.perform(post("/couple/accept"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = readResponse(result);

        assertTrue(response.get("success").asBoolean());
        assertEquals("CONNECTED", response.at("/data/status").asText());
    }

    private JsonNode readResponse(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
