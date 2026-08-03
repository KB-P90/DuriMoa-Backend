package com.project.domain.goal.importer;

import com.project.domain.goal.domain.GoalStatisticsVO;
import com.project.domain.goal.mapper.GoalStatisticsMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class GoalStatisticsCsvImporter {

    private static final Logger log = LogManager.getLogger(GoalStatisticsCsvImporter.class);
    private static final Long TEN_THOUSAND_WON = 10_000L;
    private static final List<String> EXPECTED_HEADERS = List.of(
            "지역", "하위10%", "하위25%", "중간값", "상위25%", "상위10%", "평균", "건수", "비율"
    );

    private final GoalStatisticsMapper goalStatisticsMapper;

    @Transactional
    public Integer importResource(Resource resource, String category) throws IOException {
        List<GoalStatisticsVO> statistics = parse(resource, category);
        if (statistics.isEmpty()) {
            log.warn("No complete goal statistics rows to import: {}", resource.getFilename());
            return 0;
        }
        goalStatisticsMapper.upsertGoalStatistics(statistics);
        return statistics.size();
    }

    List<GoalStatisticsVO> parse(Resource resource, String category) throws IOException {
        String normalizedCategory = normalizeCategory(category);
        List<GoalStatisticsVO> statistics = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String headerLine = reader.readLine();
            validateHeader(headerLine);

            String line;
            Integer lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) {
                    continue;
                }
                List<String> values = parseCsvLine(line);
                if (values.size() != EXPECTED_HEADERS.size()) {
                    throw new IllegalArgumentException(
                            "CSV " + lineNumber + "행의 컬럼 수가 올바르지 않습니다."
                    );
                }

                GoalStatisticsVO row = toStatistics(values, normalizedCategory);
                if (row == null) {
                    log.warn(
                            "Skipped incomplete goal statistics row. file={}, line={}, region={}",
                            resource.getFilename(),
                            lineNumber,
                            values.get(0)
                    );
                    continue;
                }
                statistics.add(row);
            }
        }
        return statistics;
    }

    private GoalStatisticsVO toStatistics(List<String> values, String category) {
        Long low10 = parseAmount(values.get(1));
        Long low25 = parseAmount(values.get(2));
        Long mid50 = parseAmount(values.get(3));
        Long high25 = parseAmount(values.get(4));
        Long high10 = parseAmount(values.get(5));
        if (low10 == null || low25 == null || mid50 == null || high25 == null || high10 == null) {
            return null;
        }
        return GoalStatisticsVO.builder()
                .region(requireText(values.get(0), "지역"))
                .category(category)
                .high10(high10)
                .high25(high25)
                .mid50(mid50)
                .low25(low25)
                .low10(low10)
                .build();
    }

    private Long parseAmount(String value) {
        String normalizedValue = value.trim().replace(",", "");
        if ("-".equals(normalizedValue)) {
            return null;
        }
        try {
            return Math.multiplyExact(Long.parseLong(normalizedValue), TEN_THOUSAND_WON);
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException("금액 형식이 올바르지 않습니다: " + value, exception);
        }
    }

    private void validateHeader(String headerLine) {
        if (headerLine == null) {
            throw new IllegalArgumentException("CSV 헤더가 없습니다.");
        }
        List<String> headers = parseCsvLine(removeBom(headerLine));
        if (!EXPECTED_HEADERS.equals(headers)) {
            throw new IllegalArgumentException("CSV 헤더 형식이 올바르지 않습니다.");
        }
    }

    private String normalizeCategory(String category) {
        return requireText(category, "카테고리");
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " 값이 비어 있습니다.");
        }
        return value.trim();
    }

    private String removeBom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;

        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (current == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    value.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                values.add(value.toString());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("CSV 따옴표가 닫히지 않았습니다.");
        }
        values.add(value.toString());
        return values;
    }
}
