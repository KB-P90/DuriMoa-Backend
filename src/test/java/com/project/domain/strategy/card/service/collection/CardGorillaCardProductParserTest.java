package com.project.domain.strategy.card.service.collection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.strategy.card.dto.CardProductCollectRequestDTO;
import com.project.domain.strategy.card.model.CardBenefitCatalog;
import com.project.domain.strategy.card.model.CardBenefitRule;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CardGorillaCardProductParserTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 목록 HTML의 링크와 스크립트에서 카드 상세 URL을 중복 없이 추출합니다.
    @Test
    void extractDetailUrlsFindsLinksAndScriptUrls() {
        CardGorillaCardProductParser parser = new CardGorillaCardProductParser();
        Document document = Jsoup.parse(
                "<html><body>"
                        + "<a href=\"/card/detail/1001?cate=CRD\">카드1</a>"
                        + "<a href=\"https://www.card-gorilla.com/card/detail/1001?cate=CRD#benefit\">중복</a>"
                        + "<script>var detail = '/card/detail/1002?cate=CRD';</script>"
                        + "</body></html>",
                "https://www.card-gorilla.com/card?cate=CRD"
        );

        List<String> detailUrls = parser.extractDetailUrls(document, 10);

        Assertions.assertEquals(2, detailUrls.size());
        Assertions.assertEquals("https://www.card-gorilla.com/card/detail/1001?cate=CRD", detailUrls.get(0));
        Assertions.assertEquals("https://www.card-gorilla.com/card/detail/1002?cate=CRD", detailUrls.get(1));
    }

    // 상세 HTML을 현재 card_product 컬럼에 맞는 수집 DTO와 계산 가능한 혜택 JSON으로 변환합니다.
    @Test
    void parseProductMapsCardGorillaHtmlToCollectRequest() {
        CardGorillaCardProductParser parser = new CardGorillaCardProductParser();
        Document document = Jsoup.parse(
                "<html><head>"
                        + "<meta property=\"og:title\" content=\"KB국민 탄탄대로 카드 | 카드고릴라\">"
                        + "<meta property=\"og:image\" content=\"https://image.example/card.png\">"
                        + "</head><body>"
                        + "<div class=\"company\">KB국민카드</div>"
                        + "<div>연회비 국내전용 1만원 해외겸용 1만2천원</div>"
                        + "<section class=\"benefit\"><ul>"
                        + "<li><strong>전 가맹점 적립</strong> 전 가맹점 1% 적립 월 1만원 한도 전월 실적 30만원 이상</li>"
                        + "<li><strong>카페 할인</strong> 카페 10% 할인 월 5천원 한도 전월 실적 30만원 이상</li>"
                        + "</ul></section>"
                        + "</body></html>",
                "https://www.card-gorilla.com/card/detail/1001?cate=CRD"
        );

        CardProductCollectRequestDTO product = parser.parseProduct(
                document,
                "https://www.card-gorilla.com/card/detail/1001?cate=CRD"
        );
        List<CardBenefitRule> rules = CardBenefitCatalog.parse(product.getBenefit(), product.getConditionText());

        Assertions.assertEquals("KB국민", product.getCompany());
        Assertions.assertEquals("KB국민 탄탄대로 카드", product.getName());
        Assertions.assertEquals(10_000, product.getAnnualFee());
        Assertions.assertEquals("https://image.example/card.png", product.getImage());
        Assertions.assertEquals("전월 30만원 이상", product.getConditionText());
        Assertions.assertEquals(2, rules.size());
        Assertions.assertEquals("ALL", rules.get(0).getCategories().get(0));
        Assertions.assertEquals(10_000L, rules.get(0).getMonthlyLimitAmount());
        Assertions.assertEquals(300_000L, rules.get(0).getMinimumPerformanceAmount());
        Assertions.assertTrue(CardBenefitCatalog.isAmountOnlyApplicable(rules.get(0)));
        Assertions.assertEquals("CAFE", rules.get(1).getCategories().get(0));
        Assertions.assertFalse(CardBenefitCatalog.isAmountOnlyApplicable(rules.get(1)));
    }

    // 카드고릴라 API JSON을 현재 card_product 컬럼과 추천 계산 규칙에 맞게 변환합니다.
    @Test
    void parseProductMapsCardGorillaApiJsonToCollectRequest() throws Exception {
        CardGorillaCardProductParser parser = new CardGorillaCardProductParser();
        JsonNode cardNode = OBJECT_MAPPER.readTree(
                "{"
                        + "\"idx\":1001,"
                        + "\"corp\":{\"name\":\"KB국민카드\"},"
                        + "\"name\":\"KB국민 모두 카드\","
                        + "\"annual_fee_basic\":\"국내전용 [10,000]원 / 해외겸용 [12,000]원\","
                        + "\"pre_month_money\":300000,"
                        + "\"card_img\":{\"url\":\"https://image.example/kb-card.png\"},"
                        + "\"key_benefit\":["
                        + "{"
                        + "\"cate_idx\":200,"
                        + "\"cate\":{\"name\":\"모든가맹점\"},"
                        + "\"title\":\"모든가맹점\","
                        + "\"comment\":\"모든가맹점 1% 적립\","
                        + "\"info\":\"<p>월 최대 1만원 적립</p>\""
                        + "},"
                        + "{"
                        + "\"cate_idx\":77,"
                        + "\"cate\":{\"name\":\"편의점\"},"
                        + "\"title\":\"편의점\","
                        + "\"comment\":\"편의점 10% 할인\","
                        + "\"info\":\"<p>전월실적 30만원 이상</p>\""
                        + "}"
                        + "]"
                        + "}"
        );

        CardProductCollectRequestDTO product = parser.parseProduct(
                cardNode,
                "https://www.card-gorilla.com/card/detail/1001"
        );
        List<CardBenefitRule> rules = CardBenefitCatalog.parse(product.getBenefit(), product.getConditionText());

        Assertions.assertEquals("KB국민", product.getCompany());
        Assertions.assertEquals("KB국민 모두 카드", product.getName());
        Assertions.assertEquals(10_000, product.getAnnualFee());
        Assertions.assertEquals("https://image.example/kb-card.png", product.getImage());
        Assertions.assertEquals("전월 30만원 이상", product.getConditionText());
        Assertions.assertEquals(2, rules.size());
        Assertions.assertEquals("ALL", rules.get(0).getCategories().get(0));
        Assertions.assertEquals(10_000L, rules.get(0).getMonthlyLimitAmount());
        Assertions.assertEquals(300_000L, rules.get(0).getMinimumPerformanceAmount());
        Assertions.assertTrue(CardBenefitCatalog.isAmountOnlyApplicable(rules.get(0)));
        Assertions.assertEquals("CONVENIENCE_STORE", rules.get(1).getCategories().get(0));
        Assertions.assertFalse(CardBenefitCatalog.isAmountOnlyApplicable(rules.get(1)));
    }
}
