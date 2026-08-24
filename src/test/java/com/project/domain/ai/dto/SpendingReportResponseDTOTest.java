package com.project.domain.ai.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SpendingReportResponseDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void serializesReportDatesAsIsoStrings() throws Exception {
        SpendingReportTrendResponseDTO trend = SpendingReportTrendResponseDTO.builder()
                .key("2026-08")
                .label("2026.8")
                .startDate(LocalDate.of(2026, 8, 1))
                .endDate(LocalDate.of(2026, 8, 23))
                .amount(100_000L)
                .transactionCount(3)
                .partialPeriod(true)
                .build();
        SpendingReportResponseDTO response = SpendingReportResponseDTO.builder()
                .analysisStartDate(LocalDate.of(2025, 9, 12))
                .analysisEndDate(LocalDate.of(2026, 8, 23))
                .dataStartDate(LocalDate.of(2025, 9, 13))
                .trend(List.of(trend))
                .categories(List.of())
                .build();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        Assertions.assertEquals("2025-09-12", json.get("analysisStartDate").asText());
        Assertions.assertEquals("2026-08-23", json.get("analysisEndDate").asText());
        Assertions.assertEquals("2025-09-13", json.get("dataStartDate").asText());
        Assertions.assertEquals("2026-08-01", json.get("trend").get(0).get("startDate").asText());
        Assertions.assertEquals("2026-08-23", json.get("trend").get(0).get("endDate").asText());
    }
}
