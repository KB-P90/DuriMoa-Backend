package com.project.domain.strategy.card.service.collection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.strategy.card.dto.CardProductCollectRequestDTO;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

@Component
public class CardGorillaCardProductCrawler {

    // 외부 수집 실패를 민감정보 없이 추적하기 위한 로거입니다.
    private static final Logger log = LogManager.getLogger(CardGorillaCardProductCrawler.class);

    // 카드고릴라 API 기본 URL입니다.
    private static final String CARD_GORILLA_API_BASE_URL = "https://api.card-gorilla.com:8080/v1";

    // 카드고릴라 카드 목록 API URL입니다.
    private static final String CARD_LIST_API_URL = CARD_GORILLA_API_BASE_URL + "/cards";

    // 카드고릴라 카드 상세 API URL prefix입니다.
    private static final String CARD_DETAIL_API_URL_PREFIX = CARD_GORILLA_API_BASE_URL + "/cards/";

    // 원본 상세 페이지 URL prefix입니다.
    private static final String CARD_DETAIL_PAGE_URL_PREFIX = "https://www.card-gorilla.com/card/detail/";

    // 한 번에 과도한 요청이 발생하지 않도록 제한하는 기본 수집 개수입니다.
    private static final Integer DEFAULT_MAX_COUNT = 100;

    // 운영 호출에서 허용하는 최대 수집 개수입니다.
    private static final Integer MAX_COUNT_LIMIT = 200;

    // 외부 페이지 요청 타임아웃입니다.
    private static final Integer REQUEST_TIMEOUT_MILLIS = 10_000;

    // 카드고릴라 웹 목록과 같은 페이지 크기입니다.
    private static final Integer PAGE_SIZE = 10;

    // 카드 상세 요청 사이에 둘 최소 대기 시간입니다.
    private static final Long DETAIL_REQUEST_INTERVAL_MILLIS = 300L;

    // 카드고릴라 API JSON을 읽는 Jackson 파서입니다.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 카드고릴라 응답 구조를 카드 상품 DTO로 바꾸는 파서입니다.
    private final CardGorillaCardProductParser parser;

    // 운영 환경에서 사용하는 크롤러 생성자입니다.
    public CardGorillaCardProductCrawler(CardGorillaCardProductParser parser) {
        this.parser = parser;
    }

    // 카드고릴라 신용카드와 체크카드 상품을 수집해 기존 collection DTO로 변환합니다.
    public List<CardProductCollectRequestDTO> crawlCardProducts(Integer maxCount, String cardType) {
        Integer safeMaxCount = normalizeMaxCount(maxCount);
        List<CardGorillaCardCategory> categories = resolveCategories(cardType);
        List<CardProductCollectRequestDTO> products = new ArrayList<>();
        for (CardGorillaCardCategory category : categories) {
            products.addAll(crawlCardProductsByCategory(category, safeMaxCount));
        }
        if (products.isEmpty()) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "카드고릴라에서 저장 가능한 카드 상품을 수집하지 못했습니다.");
        }
        return products;
    }

    // 카드고릴라 신용카드 목록과 상세 페이지를 수집해 기존 collection DTO로 변환합니다.
    public List<CardProductCollectRequestDTO> crawlCreditCardProducts(Integer maxCount) {
        return crawlCardProductsByCategory(CardGorillaCardCategory.CREDIT, normalizeMaxCount(maxCount));
    }

    // 카드고릴라 체크카드 목록과 상세 페이지를 수집해 기존 collection DTO로 변환합니다.
    public List<CardProductCollectRequestDTO> crawlCheckCardProducts(Integer maxCount) {
        return crawlCardProductsByCategory(CardGorillaCardCategory.CHECK, normalizeMaxCount(maxCount));
    }

    // 카드고릴라 특정 카드 유형의 목록과 상세 페이지를 수집해 기존 collection DTO로 변환합니다.
    private List<CardProductCollectRequestDTO> crawlCardProductsByCategory(
            CardGorillaCardCategory category,
            Integer safeMaxCount
    ) {
        List<JsonNode> cardSummaries = fetchCardSummaries(category, safeMaxCount);
        if (cardSummaries.isEmpty()) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "카드고릴라 " + category.getDisplayName() + " 목록을 찾을 수 없습니다.");
        }

        List<CardProductCollectRequestDTO> products = new ArrayList<>();
        for (JsonNode cardSummary : cardSummaries) {
            waitBeforeDetailRequest();
            try {
                Long cardIdx = extractCardIdx(cardSummary);
                JsonNode cardDetail = fetchCardDetail(cardIdx);
                products.add(parser.parseProduct(cardDetail, buildDetailPageUrl(cardIdx, category)));
            } catch (Exception exception) {
                log.warn("Card Gorilla product crawl failed. category={}, idx={}",
                        category.getCode(), extractCardIdx(cardSummary), exception);
            }
        }
        if (products.isEmpty()) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "카드고릴라 " + category.getDisplayName() + "에서 저장 가능한 카드 상품을 수집하지 못했습니다.");
        }
        return products;
    }

    // 카드고릴라 카드 목록 API를 페이지 단위로 수집합니다.
    private List<JsonNode> fetchCardSummaries(CardGorillaCardCategory category, Integer maxCount) {
        List<JsonNode> cardSummaries = new ArrayList<>();
        Integer currentPage = 1;
        while (cardSummaries.size() < maxCount) {
            JsonNode pageNode = fetchJson(CARD_LIST_API_URL, buildListParams(category, currentPage));
            JsonNode dataNode = pageNode.get("data");
            if (dataNode == null || !dataNode.isArray() || dataNode.isEmpty()) {
                break;
            }

            for (JsonNode cardNode : dataNode) {
                cardSummaries.add(cardNode);
                if (cardSummaries.size() >= maxCount) {
                    break;
                }
            }
            Integer totalCount = pageNode.path("total").asInt(0);
            if (totalCount > 0 && cardSummaries.size() >= totalCount) {
                break;
            }
            currentPage++;
        }
        return cardSummaries;
    }

    // 카드 목록 API 요청 파라미터를 구성합니다.
    private Map<String, String> buildListParams(CardGorillaCardCategory category, Integer currentPage) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("p", String.valueOf(currentPage));
        params.put("perPage", String.valueOf(PAGE_SIZE));
        params.put("cate", category.getCode());
        return params;
    }

    // 상세 API 응답을 가져옵니다. 실패하면 요약 데이터로 대체하지 않고 해당 상품 수집을 건너뜁니다.
    private JsonNode fetchCardDetail(Long cardIdx) {
        if (cardIdx == null) {
            throw new IllegalArgumentException("카드고릴라 카드 idx가 비어 있습니다.");
        }
        return fetchJson(CARD_DETAIL_API_URL_PREFIX + cardIdx, Collections.emptyMap());
    }

    // 외부 JSON 문서를 가져옵니다.
    private JsonNode fetchJson(String url, Map<String, String> params) {
        try {
            Connection connection = Jsoup.connect(url)
                    .method(Connection.Method.GET)
                    .userAgent("DuriMoa-Backend/1.0 (+https://www.card-gorilla.com/card)")
                    .referrer("https://www.card-gorilla.com/")
                    .header("Accept", "application/json")
                    .ignoreContentType(true)
                    .timeout(REQUEST_TIMEOUT_MILLIS)
                    .maxBodySize(0);
            for (Map.Entry<String, String> param : params.entrySet()) {
                connection.data(param.getKey(), param.getValue());
            }
            String body = connection.execute().body();
            return OBJECT_MAPPER.readTree(body);
        } catch (IOException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "카드고릴라 카드 정보를 수집할 수 없습니다.");
        }
    }

    // 요청 개수를 운영 가능한 범위로 정규화합니다.
    private Integer normalizeMaxCount(Integer maxCount) {
        if (maxCount == null) {
            return DEFAULT_MAX_COUNT;
        }
        if (maxCount <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "수집 개수는 1 이상이어야 합니다.");
        }
        return Math.min(maxCount, MAX_COUNT_LIMIT);
    }

    // 요청 카드 유형을 카드고릴라 카테고리 목록으로 변환합니다.
    private List<CardGorillaCardCategory> resolveCategories(String cardType) {
        if (cardType == null || cardType.trim().isEmpty() || "ALL".equalsIgnoreCase(cardType.trim())) {
            return List.of(CardGorillaCardCategory.CREDIT, CardGorillaCardCategory.CHECK);
        }
        if ("CRD".equalsIgnoreCase(cardType.trim()) || "CREDIT".equalsIgnoreCase(cardType.trim())) {
            return Collections.singletonList(CardGorillaCardCategory.CREDIT);
        }
        if ("CHK".equalsIgnoreCase(cardType.trim()) || "CHECK".equalsIgnoreCase(cardType.trim())) {
            return Collections.singletonList(CardGorillaCardCategory.CHECK);
        }
        throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "카드 유형은 ALL, CRD, CHK 중 하나여야 합니다.");
    }

    // 카드고릴라 카드 idx를 추출합니다.
    private Long extractCardIdx(JsonNode cardNode) {
        if (cardNode == null || cardNode.isNull()) {
            return null;
        }
        JsonNode idxNode = cardNode.get("idx");
        if (idxNode == null || idxNode.isNull()) {
            return null;
        }
        if (idxNode.isNumber()) {
            return idxNode.longValue();
        }
        try {
            return Long.valueOf(idxNode.asText());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    // 원본 상세 페이지 URL을 구성합니다.
    private String buildDetailPageUrl(Long cardIdx, CardGorillaCardCategory category) {
        if (cardIdx == null) {
            return "https://www.card-gorilla.com/card?cate=" + category.getCode();
        }
        return CARD_DETAIL_PAGE_URL_PREFIX + cardIdx;
    }

    // 외부 상세 페이지 연속 요청 사이에 짧은 대기 시간을 둡니다.
    private void waitBeforeDetailRequest() {
        try {
            Thread.sleep(DETAIL_REQUEST_INTERVAL_MILLIS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "카드고릴라 수집 작업이 중단되었습니다.");
        }
    }

    // 카드고릴라 카드 목록 구분값입니다.
    private enum CardGorillaCardCategory {
        CREDIT("CRD", "신용카드"),
        CHECK("CHK", "체크카드");

        private final String code;
        private final String displayName;

        CardGorillaCardCategory(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }

        private String getCode() {
            return code;
        }

        private String getDisplayName() {
            return displayName;
        }
    }
}
