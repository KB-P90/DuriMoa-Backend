package com.project.domain.couple.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.couple.dto.CoupleInvitationDTO;
import com.project.domain.couple.service.CoupleInvitationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CoupleControllerTest {

    private static final Long AUTHENTICATED_USER_ID = 3L;

    @Mock
    private CoupleInvitationService coupleInvitationService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        CoupleController coupleController = new CoupleController(coupleInvitationService);
        mockMvc = MockMvcBuilders.standaloneSetup(coupleController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        objectMapper = new ObjectMapper();

        // JwtAuthenticationFilter가 실제로 SecurityContext에 넣는 것과 동일한 형태(principal = Long userId)로 인증을 흉내낸다.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(AUTHENTICATED_USER_ID, null, Collections.emptyList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void statusReturnsListOfMyCoupleRelationships() throws Exception {
        when(coupleInvitationService.getStatuses(AUTHENTICATED_USER_ID))
                .thenReturn(List.of(
                        new CoupleInvitationDTO(2L, "이유진", "B", "WAIT"),
                        new CoupleInvitationDTO(5L, "박서연", "B", "WAIT")));

        MvcResult result = mockMvc.perform(get("/api/couples/status"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = readResponse(result);

        assertTrue(response.get("success").asBoolean());
        assertEquals(2, response.get("data").size());
        assertEquals("이유진", response.at("/data/0/name").asText());
        assertEquals("박서연", response.at("/data/1/name").asText());
    }

    @Test
    void inviteReturnsConnectionResult() throws Exception {
        when(coupleInvitationService.invite(eq(AUTHENTICATED_USER_ID), eq("XYZ789")))
                .thenReturn(new CoupleInvitationDTO(2L, "이유진", "B", "WAIT"));

        MvcResult result = mockMvc.perform(post("/api/couples/invite")
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
    void acceptConfirmsSpecifiedPartnerRequest() throws Exception {
        when(coupleInvitationService.accept(eq(AUTHENTICATED_USER_ID), eq(1L)))
                .thenReturn(new CoupleInvitationDTO(1L, "김주영", "G", "CONNECTED"));

        MvcResult result = mockMvc.perform(post("/api/couples/accept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"partnerUserId\":1}"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = readResponse(result);

        assertTrue(response.get("success").asBoolean());
        assertEquals("CONNECTED", response.at("/data/status").asText());
    }

    @Test
    void disconnectReturnsDisconnectedStatus() throws Exception {
        when(coupleInvitationService.disconnect(AUTHENTICATED_USER_ID))
                .thenReturn(new CoupleInvitationDTO(2L, "이유진", "B", "DISCONNECTED"));

        MvcResult result = mockMvc.perform(post("/api/couples/disconnect"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode response = readResponse(result);

        assertTrue(response.get("success").asBoolean());
        assertEquals("DISCONNECTED", response.at("/data/status").asText());
    }

    private JsonNode readResponse(MvcResult result) throws Exception {
        return objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
