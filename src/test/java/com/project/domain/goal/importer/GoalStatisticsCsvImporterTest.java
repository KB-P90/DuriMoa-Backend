package com.project.domain.goal.importer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.project.domain.goal.domain.GoalStatisticsVO;
import com.project.domain.goal.mapper.GoalStatisticsMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

class GoalStatisticsCsvImporterTest {

    @Test
    @SuppressWarnings("unchecked")
    void importResourceConvertsTenThousandWonAndSkipsIncompleteRows() throws Exception {
        GoalStatisticsMapper mapper = mock(GoalStatisticsMapper.class);
        GoalStatisticsCsvImporter importer = new GoalStatisticsCsvImporter(mapper);
        when(mapper.upsertGoalStatistics(anyList())).thenReturn(1);

        String csv = "\"지역\",\"하위10%\",\"하위25%\",\"중간값\",\"상위25%\","
                + "\"상위10%\",\"평균\",\"건수\",\"비율\"\n"
                + "\"서울(강남)\",\"212\",\"440\",\"750\",\"920\",\"1,300\","
                + "\"750\",\"625\",\"12.1%\"\n"
                + "\"제주도\",\"128\",\"128\",\"140\",\"-\",\"-\",\"140\",\"3\",\"0.7%\"\n";
        ByteArrayResource resource =
                new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8)) {
                    @Override
                    public String getFilename() {
                        return "예식장.csv";
                    }
                };

        Integer importedCount = importer.importResource(resource, "예식장");

        assertEquals(1, importedCount);
        ArgumentCaptor<List<GoalStatisticsVO>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper).upsertGoalStatistics(captor.capture());
        GoalStatisticsVO imported = captor.getValue().get(0);
        assertEquals("서울(강남)", imported.getRegion());
        assertEquals("예식장", imported.getCategory());
        assertEquals(13_000_000L, imported.getHigh10());
        assertEquals(9_200_000L, imported.getHigh25());
        assertEquals(7_500_000L, imported.getMid50());
        assertEquals(4_400_000L, imported.getLow25());
        assertEquals(2_120_000L, imported.getLow10());
    }

    @Test
    void parseLoadsAllBundledCategoryFiles() throws Exception {
        GoalStatisticsCsvImporter importer =
                new GoalStatisticsCsvImporter(mock(GoalStatisticsMapper.class));

        assertEquals(
                17,
                importer.parse(
                        new ClassPathResource("data/wedding-statistics/예식장.csv"),
                        "예식장"
                ).size()
        );
        assertEquals(
                17,
                importer.parse(
                        new ClassPathResource("data/wedding-statistics/스튜디오.csv"),
                        "스튜디오"
                ).size()
        );
        assertEquals(
                17,
                importer.parse(
                        new ClassPathResource("data/wedding-statistics/드레스.csv"),
                        "드레스"
                ).size()
        );
        assertEquals(
                17,
                importer.parse(
                        new ClassPathResource("data/wedding-statistics/메이크업.csv"),
                        "메이크업"
                ).size()
        );
    }

    @Test
    void parseUsesPreviousPercentileForBundledMissingValues() throws Exception {
        GoalStatisticsCsvImporter importer =
                new GoalStatisticsCsvImporter(mock(GoalStatisticsMapper.class));

        List<GoalStatisticsVO> dressStatistics = importer.parse(
                new ClassPathResource("data/wedding-statistics/드레스.csv"),
                "드레스"
        );
        GoalStatisticsVO jejuDress = dressStatistics.stream()
                .filter(statistics -> "제주도".equals(statistics.getRegion()))
                .findFirst()
                .orElseThrow();

        assertEquals(1_400_000L, jejuDress.getMid50());
        assertEquals(1_400_000L, jejuDress.getHigh25());
        assertEquals(1_400_000L, jejuDress.getHigh10());
    }
}
