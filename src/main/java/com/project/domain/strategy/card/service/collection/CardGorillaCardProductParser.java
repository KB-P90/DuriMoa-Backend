package com.project.domain.strategy.card.service.collection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.strategy.card.dto.CardProductCollectRequestDTO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

@Component
public class CardGorillaCardProductParser {

    // 카드고릴라 카드 상세 페이지 URL을 찾는 패턴입니다.
    private static final Pattern DETAIL_URL_PATTERN = Pattern.compile(
            "(https://www\\.card-gorilla\\.com)?/card/detail[^\"'\\s<]*"
    );

    // 원 단위 금액 또는 만/천 원 단위 금액을 찾는 패턴입니다.
    private static final Pattern MONEY_PATTERN = Pattern.compile(
            "(\\d[\\d,]*)(?:\\s*만)?(?:\\s*\\d?\\s*천)?\\s*원"
    );

    // 혜택률 추출 패턴입니다.
    private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%");

    // 전월 실적 조건 추출 패턴입니다.
    private static final Pattern PERFORMANCE_PATTERN = Pattern.compile(
            "전월\\s*(?:실적|이용금액)?\\s*(\\d[\\d,]*)(?:\\s*만)?\\s*원\\s*이상"
    );

    // 카드고릴라 API 상세 혜택에서 월 한도 금액을 찾는 패턴입니다.
    private static final Pattern MONTHLY_LIMIT_PATTERN = Pattern.compile(
            "(?:월|매월|월간)\\s*(?:최대\\s*)?"
                    + "(\\d[\\d,]*(?:\\s*만)?(?:\\s*\\d?\\s*천)?\\s*원)"
                    + "\\s*(?:한도|까지|할인|적립|캐시백|제공)?"
    );

    // 혜택 한도 문구에서 금액을 찾는 패턴입니다.
    private static final Pattern LIMIT_AMOUNT_PATTERN = Pattern.compile(
            "(\\d[\\d,]*(?:\\s*만)?(?:\\s*\\d?\\s*천)?\\s*원)\\s*(?:한도|까지)"
    );

    // 카드고릴라의 구간형 혜택 설명에서 콜론 뒤의 혜택 금액을 찾는 패턴입니다.
    private static final Pattern COLON_AMOUNT_PATTERN = Pattern.compile(
            "[:：]\\s*(\\d[\\d,]*(?:\\s*만)?(?:\\s*\\d?\\s*천)?\\s*원)"
    );

    // DB JSON 컬럼에 저장할 혜택 원문을 구성하는 Jackson 파서입니다.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 카드 상세 URL을 목록 HTML의 링크와 스크립트 문자열에서 추출합니다.
    public List<String> extractDetailUrls(Document document, Integer maxCount) {
        if (document == null) {
            return Collections.emptyList();
        }
        Set<String> detailUrls = new LinkedHashSet<>();
        for (Element link : document.select("a[href]")) {
            String url = link.absUrl("href");
            if (isCardDetailUrl(url)) {
                detailUrls.add(stripFragment(url));
            }
        }

        Matcher matcher = DETAIL_URL_PATTERN.matcher(document.html());
        while (matcher.find()) {
            String url = matcher.group();
            if (url.startsWith("/")) {
                url = "https://www.card-gorilla.com" + url;
            }
            if (isCardDetailUrl(url)) {
                detailUrls.add(stripFragment(url));
            }
        }

        return detailUrls.stream()
                .limit(normalizeLimit(maxCount))
                .collect(Collectors.toList());
    }

    // 상세 HTML을 기존 카드 상품 수집 DTO로 매핑합니다.
    public CardProductCollectRequestDTO parseProduct(Document document, String sourceUrl) {
        if (document == null) {
            throw new IllegalArgumentException("카드 상세 HTML이 비어 있습니다.");
        }
        String pageText = normalizeText(document.text());
        String name = extractCardName(document);
        String company = extractCompany(document, name, pageText);
        if (company == null || company.trim().isEmpty()) {
            throw new IllegalArgumentException("카드사를 찾을 수 없습니다.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("카드명을 찾을 수 없습니다.");
        }

        return CardProductCollectRequestDTO.builder()
                .company(company)
                .name(name)
                .annualFee(extractAnnualFee(pageText))
                .image(extractImage(document))
                .benefit(buildBenefitJson(document, sourceUrl))
                .conditionText(extractConditionText(pageText))
                .build();
    }

    // 카드고릴라 API 응답을 기존 카드 상품 수집 DTO로 매핑합니다.
    public CardProductCollectRequestDTO parseProduct(JsonNode cardNode, String sourceUrl) {
        if (cardNode == null || cardNode.isNull() || !cardNode.isObject()) {
            throw new IllegalArgumentException("카드고릴라 카드 JSON이 비어 있습니다.");
        }
        String name = firstText(cardNode, "name");
        String company = extractApiCompany(cardNode);
        if (company == null || company.trim().isEmpty()) {
            throw new IllegalArgumentException("카드사를 찾을 수 없습니다.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("카드명을 찾을 수 없습니다.");
        }

        return CardProductCollectRequestDTO.builder()
                .company(company)
                .name(normalizeText(name))
                .annualFee(extractAnnualFee(cardNode))
                .image(extractApiImage(cardNode))
                .benefit(buildBenefitJson(cardNode, sourceUrl))
                .conditionText(extractApiConditionText(cardNode))
                .build();
    }

    // 상세 URL 여부를 확인합니다.
    private Boolean isCardDetailUrl(String url) {
        return url != null
                && url.startsWith("https://www.card-gorilla.com/card/detail");
    }

    // URL fragment를 제거해 같은 상세 페이지 중복을 줄입니다.
    private String stripFragment(String url) {
        int fragmentIndex = url.indexOf("#");
        if (fragmentIndex < 0) {
            return url;
        }
        return url.substring(0, fragmentIndex);
    }

    // limit 값을 안전하게 정규화합니다.
    private Integer normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return Integer.MAX_VALUE;
        }
        return limit;
    }

    // 카드명을 메타 태그와 주요 제목 후보에서 추출합니다.
    private String extractCardName(Document document) {
        String name = firstText(document,
                "meta[property=og:title]",
                "meta[name=title]",
                "h1",
                ".card_name",
                ".card-name",
                ".name",
                ".tit",
                ".title"
        );
        return cleanCardName(name);
    }

    // 카드명을 카드고릴라 페이지 타이틀 표기에서 정리합니다.
    private String cleanCardName(String name) {
        if (name == null) {
            return null;
        }
        String cleanedName = normalizeText(name)
                .replace("카드고릴라", "")
                .replace("CARD GORILLA", "")
                .replaceAll("\\s*[|>-]\\s*$", "")
                .trim();
        if (cleanedName.contains("|")) {
            cleanedName = cleanedName.substring(0, cleanedName.indexOf("|")).trim();
        }
        return cleanedName.isEmpty() ? null : cleanedName;
    }

    // 카드사를 명시 셀렉터와 카드명/본문 키워드에서 추출합니다.
    private String extractCompany(Document document, String name, String pageText) {
        String company = firstText(document,
                ".card_company",
                ".card-company",
                ".company",
                ".brand",
                ".issuer",
                ".cardcorp",
                ".card-corp"
        );
        String normalizedCompany = normalizeCompany(company);
        if (normalizedCompany != null) {
            return normalizedCompany;
        }
        normalizedCompany = normalizeCompany(name);
        if (normalizedCompany != null) {
            return normalizedCompany;
        }
        return normalizeCompany(pageText);
    }

    // 국내 카드사 표기를 DB에 저장할 카드사명으로 정규화합니다.
    private String normalizeCompany(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String normalizedText = normalizeText(text);
        Map<String, String> companyKeywords = new LinkedHashMap<>();
        companyKeywords.put("KB국민", "KB국민");
        companyKeywords.put("국민카드", "KB국민");
        companyKeywords.put("신한카드", "신한");
        companyKeywords.put("신한", "신한");
        companyKeywords.put("삼성카드", "삼성");
        companyKeywords.put("삼성", "삼성");
        companyKeywords.put("현대카드", "현대");
        companyKeywords.put("현대", "현대");
        companyKeywords.put("롯데카드", "롯데");
        companyKeywords.put("롯데", "롯데");
        companyKeywords.put("하나카드", "하나");
        companyKeywords.put("하나", "하나");
        companyKeywords.put("우리카드", "우리");
        companyKeywords.put("우리", "우리");
        companyKeywords.put("NH농협", "NH농협");
        companyKeywords.put("농협카드", "NH농협");
        companyKeywords.put("IBK", "IBK기업");
        companyKeywords.put("기업은행", "IBK기업");
        companyKeywords.put("BC카드", "BC");
        companyKeywords.put("비씨카드", "BC");
        companyKeywords.put("카카오뱅크", "카카오뱅크");
        companyKeywords.put("토스뱅크", "토스뱅크");
        companyKeywords.put("케이뱅크", "케이뱅크");
        companyKeywords.put("우체국", "우체국");
        companyKeywords.put("수협", "수협");
        companyKeywords.put("씨티", "씨티");
        companyKeywords.put("광주", "광주");
        companyKeywords.put("전북", "전북");
        companyKeywords.put("제주", "제주");

        for (Map.Entry<String, String> entry : companyKeywords.entrySet()) {
            if (normalizedText.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    // 카드 이미지 URL을 메타 태그와 이미지 후보에서 추출합니다.
    private String extractImage(Document document) {
        String image = firstText(document,
                "meta[property=og:image]",
                "meta[name=twitter:image]"
        );
        if (image != null && !image.trim().isEmpty()) {
            return image.trim();
        }
        Element imageElement = document.selectFirst("img[src*=/card], img[src*=card], .card_img img, .card-image img");
        if (imageElement == null) {
            return null;
        }
        String imageUrl = imageElement.absUrl("src");
        return imageUrl == null || imageUrl.trim().isEmpty() ? null : imageUrl;
    }

    // 본문에서 연회비를 추출합니다. 여러 금액이 있으면 낮은 연회비를 대표값으로 저장합니다.
    private Integer extractAnnualFee(String pageText) {
        String annualFeeText = snippetAfterKeyword(pageText, "연회비", 120);
        if (annualFeeText == null) {
            return 0;
        }
        String normalizedText = cutBeforeBenefitSection(normalizeText(annualFeeText));
        if (normalizedText.contains("무료") || normalizedText.contains("없음")) {
            return 0;
        }
        List<Long> amounts = parseMoneyAmounts(normalizedText);
        return amounts.stream()
                .filter(amount -> amount != null && amount >= 0)
                .min(Long::compareTo)
                .map(Long::intValue)
                .orElse(0);
    }

    // 카드고릴라 API 응답의 연회비 문구에서 대표 연회비를 추출합니다.
    private Integer extractAnnualFee(JsonNode cardNode) {
        String annualFeeText = firstText(cardNode, "annual_fee_basic", "annual_fee_detail");
        if (annualFeeText == null || annualFeeText.trim().isEmpty()) {
            return 0;
        }
        String normalizedText = normalizeText(annualFeeText);
        if (normalizedText.contains("무료") || normalizedText.contains("없음")) {
            return 0;
        }
        return parseMoneyAmounts(normalizedText).stream()
                .filter(amount -> amount != null && amount >= 0)
                .min(Long::compareTo)
                .map(Long::intValue)
                .orElse(0);
    }

    // 본문에서 전월 실적 조건을 추출합니다.
    private String extractConditionText(String pageText) {
        if (pageText == null || pageText.trim().isEmpty()) {
            return null;
        }
        String normalizedText = normalizeText(pageText);
        if (normalizedText.contains("전월실적 없음") || normalizedText.contains("전월 실적 없음")) {
            return "전월실적 없음";
        }
        Matcher matcher = PERFORMANCE_PATTERN.matcher(normalizedText);
        if (matcher.find()) {
            Long amount = toWonAmount(matcher.group());
            if (amount != null) {
                return "전월 " + formatWonAmount(amount) + " 이상";
            }
        }
        return null;
    }

    // 카드고릴라 API 응답의 전월 실적 금액을 조건 문구로 변환합니다.
    private String extractApiConditionText(JsonNode cardNode) {
        Long minimumSpendAmount = firstLong(cardNode, "pre_month_money");
        if (minimumSpendAmount == null) {
            return null;
        }
        if (minimumSpendAmount <= 0L) {
            return "전월실적 없음";
        }
        return "전월 " + formatWonAmount(minimumSpendAmount) + " 이상";
    }

    // 카드 상세 HTML에서 계산 가능한 혜택 JSON 문자열을 구성합니다.
    private String buildBenefitJson(Document document, String sourceUrl) {
        List<String> benefitTexts = extractBenefitTexts(document);
        List<Map<String, Object>> benefits = new ArrayList<>();
        String pageText = normalizeText(document.text());
        Long defaultMinimumSpend = extractMinimumSpendAmount(pageText);

        for (String benefitText : benefitTexts) {
            BigDecimal benefitRate = parsePercentRate(benefitText);
            if (benefitRate == null) {
                continue;
            }
            Map<String, Object> benefit = new LinkedHashMap<>();
            benefit.put("benefit_name", summarizeBenefitName(benefitText));
            benefit.put("description", benefitText);
            benefit.put("categories", inferCategories(benefitText));
            benefit.put("benefit_rate", benefitRate);

            Long monthlyLimitAmount = extractMonthlyLimitAmount(benefitText);
            if (monthlyLimitAmount != null) {
                benefit.put("monthly_limit_amount", monthlyLimitAmount);
            }
            Long minimumSpend = extractMinimumSpendAmount(benefitText);
            if (minimumSpend == null) {
                minimumSpend = defaultMinimumSpend;
            }
            if (minimumSpend != null) {
                benefit.put("minimum_monthly_spend", minimumSpend);
                if (minimumSpend > 0) {
                    benefit.put("benefit_thresholds", Collections.singletonList(minimumSpend));
                }
            }
            benefits.add(benefit);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("source_site", "CARD_GORILLA");
        root.put("source_url", sourceUrl);
        root.put("benefits", benefits);
        try {
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("카드 혜택 정보를 JSON으로 변환할 수 없습니다.");
        }
    }

    // 카드고릴라 API 응답에서 계산 가능한 혜택 JSON 문자열을 구성합니다.
    private String buildBenefitJson(JsonNode cardNode, String sourceUrl) {
        List<Map<String, Object>> benefits = new ArrayList<>();
        Long defaultMinimumSpend = firstLong(cardNode, "pre_month_money");
        appendKeyBenefits(benefits, cardNode.get("key_benefit"), defaultMinimumSpend);
        if (benefits.isEmpty()) {
            appendTopBenefits(benefits, cardNode.get("top_benefit"), defaultMinimumSpend);
        }

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("source_site", "CARD_GORILLA");
        root.put("source_url", sourceUrl);
        Long sourceIdx = firstLong(cardNode, "idx");
        if (sourceIdx != null) {
            root.put("source_card_idx", sourceIdx);
        }
        root.put("benefits", benefits);
        try {
            return OBJECT_MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("카드 혜택 정보를 JSON으로 변환할 수 없습니다.");
        }
    }

    // 상세 혜택 배열을 표준 혜택 JSON 항목으로 변환합니다.
    private void appendKeyBenefits(List<Map<String, Object>> benefits, JsonNode benefitNodes, Long defaultMinimumSpend) {
        if (benefitNodes == null || !benefitNodes.isArray()) {
            return;
        }
        for (JsonNode benefitNode : benefitNodes) {
            Map<String, Object> benefit = buildKeyBenefit(benefitNode, defaultMinimumSpend);
            if (benefit != null) {
                benefits.add(benefit);
            }
        }
    }

    // 목록 대표 혜택 배열을 표준 혜택 JSON 항목으로 변환합니다.
    private void appendTopBenefits(List<Map<String, Object>> benefits, JsonNode benefitNodes, Long defaultMinimumSpend) {
        if (benefitNodes == null || !benefitNodes.isArray()) {
            return;
        }
        for (JsonNode benefitNode : benefitNodes) {
            Map<String, Object> benefit = buildTopBenefit(benefitNode, defaultMinimumSpend);
            if (benefit != null) {
                benefits.add(benefit);
            }
        }
    }

    // 상세 혜택 한 건을 계산 가능한 표준 혜택 항목으로 변환합니다.
    private Map<String, Object> buildKeyBenefit(JsonNode benefitNode, Long defaultMinimumSpend) {
        String title = firstNonBlank(
                firstText(benefitNode, "title"),
                firstText(benefitNode.get("cate"), "name"),
                firstText(benefitNode, "comment")
        );
        String comment = firstText(benefitNode, "comment");
        String infoText = htmlToText(firstText(benefitNode, "info"));
        String benefitText = joinTexts(title, comment, infoText);
        BigDecimal benefitRate = parsePercentRate(benefitText);
        if (benefitRate == null) {
            return null;
        }

        Map<String, Object> benefit = buildBenefitMap(title, benefitText, benefitRate, defaultMinimumSpend);
        Long cateIdx = firstLong(benefitNode, "cate_idx");
        if (cateIdx != null) {
            benefit.put("source_benefit_idx", cateIdx);
        }
        return benefit;
    }

    // 대표 혜택 한 건을 계산 가능한 표준 혜택 항목으로 변환합니다.
    private Map<String, Object> buildTopBenefit(JsonNode benefitNode, Long defaultMinimumSpend) {
        String title = firstText(benefitNode, "title");
        List<String> tags = parseTextArray(benefitNode.get("tags"));
        String benefitText = joinTexts(title, String.join(" ", tags), firstText(benefitNode, "inputValue"));
        BigDecimal benefitRate = parsePercentRate(benefitText);
        if (benefitRate == null) {
            return null;
        }

        Map<String, Object> benefit = buildBenefitMap(title, benefitText, benefitRate, defaultMinimumSpend);
        Long benefitIdx = firstLong(benefitNode, "idx");
        if (benefitIdx != null) {
            benefit.put("source_benefit_idx", benefitIdx);
        }
        return benefit;
    }

    // 계산 규칙 파서가 읽을 수 있는 혜택 항목 Map을 구성합니다.
    private Map<String, Object> buildBenefitMap(
            String title,
            String benefitText,
            BigDecimal benefitRate,
            Long defaultMinimumSpend
    ) {
        Map<String, Object> benefit = new LinkedHashMap<>();
        benefit.put("benefit_name", summarizeBenefitName(firstNonBlank(title, benefitText)));
        benefit.put("description", limitText(benefitText, 1000));
        benefit.put("categories", inferCategories(benefitText));
        benefit.put("benefit_rate", benefitRate);

        Long monthlyLimitAmount = extractMonthlyLimitAmount(benefitText);
        if (monthlyLimitAmount != null) {
            benefit.put("monthly_limit_amount", monthlyLimitAmount);
        }
        Long minimumSpend = extractMinimumSpendAmount(benefitText);
        if (minimumSpend == null) {
            minimumSpend = defaultMinimumSpend;
        }
        if (minimumSpend != null) {
            benefit.put("minimum_monthly_spend", minimumSpend);
            if (minimumSpend > 0L) {
                benefit.put("benefit_thresholds", Collections.singletonList(minimumSpend));
            }
        }
        return benefit;
    }

    // 혜택 후보 텍스트를 HTML 블록과 본문에서 추출합니다.
    private List<String> extractBenefitTexts(Document document) {
        Set<String> benefitTexts = new LinkedHashSet<>();
        Elements elements = document.select(
                ".benefit li, .benefits li, .card_benefit li, .card-benefit li, "
                        + ".bene_area li, .bnf li, .service li, .info-benefit li, "
                        + ".card_detail li, .card-detail li, .view_con li, dl, tr"
        );
        for (Element element : elements) {
            addBenefitText(benefitTexts, element.text());
        }

        if (benefitTexts.isEmpty()) {
            String[] parts = normalizeText(document.text()).split("(?<=원)|(?<=%)|[.!?]");
            for (String part : parts) {
                addBenefitText(benefitTexts, part);
            }
        }
        return benefitTexts.stream()
                .limit(30)
                .collect(Collectors.toList());
    }

    // 혜택으로 볼 수 있는 텍스트만 중복 없이 추가합니다.
    private void addBenefitText(Set<String> benefitTexts, String text) {
        String normalizedText = normalizeText(text);
        if (normalizedText.length() < 5 || normalizedText.length() > 350) {
            return;
        }
        if (!PERCENT_PATTERN.matcher(normalizedText).find()) {
            return;
        }
        if (!containsBenefitKeyword(normalizedText)) {
            return;
        }
        benefitTexts.add(normalizedText);
    }

    // 할인·적립·캐시백 등 혜택 키워드가 있는지 확인합니다.
    private Boolean containsBenefitKeyword(String text) {
        return text.contains("할인")
                || text.contains("적립")
                || text.contains("캐시백")
                || text.contains("포인트")
                || text.contains("마일")
                || text.contains("청구할인");
    }

    // 혜택 설명의 앞부분을 혜택 이름으로 사용합니다.
    private String summarizeBenefitName(String benefitText) {
        String[] splitTexts = benefitText.split("\\s{2,}|[:：]");
        String summary = splitTexts.length == 0 ? benefitText : splitTexts[0];
        if (summary.length() > 40) {
            return summary.substring(0, 40);
        }
        return summary;
    }

    // 혜택 설명에서 소비 카테고리를 추론합니다.
    private List<String> inferCategories(String text) {
        String normalizedText = normalizeText(text);
        Set<String> categories = new LinkedHashSet<>();
        if (isUniversalBenefit(normalizedText)) {
            categories.add("ALL");
        }
        addCategoryIfMatched(categories, normalizedText, "CAFE", "카페", "커피", "스타벅스", "이디야", "투썸");
        addCategoryIfMatched(categories, normalizedText, "TRANSPORT", "대중교통", "버스", "지하철", "택시", "교통");
        addCategoryIfMatched(categories, normalizedText, "FUEL", "주유", "충전소", "LPG");
        addCategoryIfMatched(categories, normalizedText, "MART", "마트", "이마트", "홈플러스", "롯데마트");
        addCategoryIfMatched(categories, normalizedText, "FURNITURE", "가구", "혼수", "가전", "전자제품", "하이마트",
                "전자랜드", "LG전자", "삼성스토어", "오늘의집", "이케아", "IKEA", "인테리어");
        addCategoryIfMatched(categories, normalizedText, "CONVENIENCE_STORE", "편의점", "CU", "GS25", "세븐일레븐");
        addCategoryIfMatched(categories, normalizedText, "ONLINE_SHOPPING", "온라인", "쇼핑", "쿠팡", "네이버페이", "11번가", "G마켓");
        addCategoryIfMatched(categories, normalizedText, "DELIVERY", "배달", "배민", "요기요", "쿠팡이츠");
        addCategoryIfMatched(categories, normalizedText, "MEDICAL", "병원", "약국", "의료");
        addCategoryIfMatched(categories, normalizedText, "TELECOM", "통신", "휴대폰", "이동통신", "SKT", "KT", "LG U+");
        addCategoryIfMatched(categories, normalizedText, "MOVIE", "영화", "CGV", "롯데시네마", "메가박스");
        addCategoryIfMatched(categories, normalizedText, "DINING", "외식", "음식점", "레스토랑", "패밀리레스토랑");
        addCategoryIfMatched(categories, normalizedText, "TRAVEL", "여행", "항공", "호텔", "숙박");
        addCategoryIfMatched(categories, normalizedText, "EDUCATION", "교육", "학원", "서점");
        addCategoryIfMatched(categories, normalizedText, "WEDDING", "웨딩", "결혼", "예식");
        addCategoryIfMatched(categories, normalizedText, "OVERSEAS", "해외", "국외");
        return new ArrayList<>(categories);
    }

    // 키워드가 있으면 카테고리를 추가합니다.
    private void addCategoryIfMatched(Set<String> categories, String text, String category, String... keywords) {
        for (String keyword : keywords) {
            if (text.toUpperCase(Locale.ROOT).contains(keyword.toUpperCase(Locale.ROOT))) {
                categories.add(category);
                return;
            }
        }
    }

    // 전 가맹점 또는 기본 혜택 문구인지 확인합니다.
    private Boolean isUniversalBenefit(String text) {
        String normalizedText = text.replaceAll("[\\s_\\-·/]", "");
        return normalizedText.contains("전가맹점")
                || normalizedText.contains("전체가맹점")
                || normalizedText.contains("국내외가맹점")
                || normalizedText.contains("모든가맹점")
                || normalizedText.contains("기본할인")
                || normalizedText.contains("기본적립")
                || normalizedText.contains("기본혜택");
    }

    // 혜택률을 퍼센트 숫자로 추출합니다.
    private BigDecimal parsePercentRate(String text) {
        Matcher matcher = PERCENT_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return new BigDecimal(matcher.group(1));
    }

    // 월 한도 문구 주변 금액을 추출합니다.
    private Long extractMonthlyLimitAmount(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String normalizedText = normalizeText(text);
        Matcher monthlyLimitMatcher = MONTHLY_LIMIT_PATTERN.matcher(normalizedText);
        if (monthlyLimitMatcher.find()) {
            return toWonAmount(monthlyLimitMatcher.group(1));
        }

        String limitText = snippetAroundKeyword(normalizedText, "한도", 120);
        if (limitText == null) {
            return null;
        }
        Matcher colonAmountMatcher = COLON_AMOUNT_PATTERN.matcher(limitText);
        if (colonAmountMatcher.find()) {
            return toWonAmount(colonAmountMatcher.group(1));
        }
        Matcher limitAmountMatcher = LIMIT_AMOUNT_PATTERN.matcher(limitText);
        if (limitAmountMatcher.find()) {
            return toWonAmount(limitAmountMatcher.group(1));
        }
        return null;
    }

    // 전월 실적 금액을 추출합니다.
    private Long extractMinimumSpendAmount(String text) {
        if (text == null) {
            return null;
        }
        String normalizedText = normalizeText(text);
        if (normalizedText.contains("전월실적 없음") || normalizedText.contains("전월 실적 없음")) {
            return 0L;
        }
        Matcher matcher = PERFORMANCE_PATTERN.matcher(normalizedText);
        if (!matcher.find()) {
            return null;
        }
        return toWonAmount(matcher.group());
    }

    // 텍스트에 포함된 금액 목록을 원 단위로 변환합니다.
    private List<Long> parseMoneyAmounts(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String normalizedText = text.replace("[", "").replace("]", "");
        List<Long> amounts = new ArrayList<>();
        Matcher matcher = MONEY_PATTERN.matcher(normalizedText);
        while (matcher.find()) {
            Long amount = toWonAmount(matcher.group());
            if (amount != null) {
                amounts.add(amount);
            }
        }
        return amounts;
    }

    // 금액 텍스트를 원 단위로 변환합니다.
    private Long toWonAmount(String moneyText) {
        if (moneyText == null) {
            return null;
        }
        String normalizedText = moneyText.replace(",", "").replaceAll("\\s+", "");
        if (normalizedText.contains("만")) {
            String[] tenThousandParts = normalizedText.split("만", 2);
            Long amount = parseDigits(tenThousandParts[0]);
            if (amount == null) {
                return null;
            }
            amount *= 10_000L;
            if (tenThousandParts.length > 1 && tenThousandParts[1].contains("천")) {
                Long thousandAmount = parseDigits(tenThousandParts[1].substring(0, tenThousandParts[1].indexOf("천")));
                if (thousandAmount != null) {
                    amount += thousandAmount * 1_000L;
                }
            }
            return amount;
        }
        if (normalizedText.contains("천")) {
            Long amount = parseDigits(normalizedText.substring(0, normalizedText.indexOf("천")));
            return amount == null ? null : amount * 1_000L;
        }
        return parseDigits(normalizedText);
    }

    // 문자열에서 첫 번째 숫자 값을 추출합니다.
    private Long parseDigits(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = Pattern.compile("\\d+").matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return Long.valueOf(matcher.group());
    }

    // 원 단위 금액을 조건 문구에 넣을 형식으로 변환합니다.
    private String formatWonAmount(Long amount) {
        if (amount != null && amount % 10_000L == 0) {
            return (amount / 10_000L) + "만원";
        }
        return amount + "원";
    }

    // 키워드 이후 일부 텍스트를 반환합니다.
    private String snippetAfterKeyword(String text, String keyword, Integer length) {
        if (text == null || keyword == null) {
            return null;
        }
        int index = text.indexOf(keyword);
        if (index < 0) {
            return null;
        }
        int endIndex = Math.min(text.length(), index + length);
        return text.substring(index, endIndex);
    }

    // 연회비 주변 텍스트가 혜택 영역까지 이어질 경우 혜택 문구 앞에서 끊습니다.
    private String cutBeforeBenefitSection(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }
        List<String> benefitKeywords = List.of("혜택", "전 가맹점", "할인", "적립", "캐시백", "포인트", "카페");
        Integer cutIndex = null;
        for (String keyword : benefitKeywords) {
            int index = text.indexOf(keyword);
            if (index > 0 && (cutIndex == null || index < cutIndex)) {
                cutIndex = index;
            }
        }
        if (cutIndex == null) {
            return text;
        }
        return text.substring(0, cutIndex);
    }

    // 키워드 앞뒤 일부 텍스트를 반환합니다.
    private String snippetAroundKeyword(String text, String keyword, Integer length) {
        if (text == null || keyword == null) {
            return null;
        }
        int index = text.indexOf(keyword);
        if (index < 0) {
            return null;
        }
        int startIndex = Math.max(0, index - length);
        int endIndex = Math.min(text.length(), index + length);
        return text.substring(startIndex, endIndex);
    }

    // 카드고릴라 API 응답의 카드사명을 정규화합니다.
    private String extractApiCompany(JsonNode cardNode) {
        String company = firstNonBlank(
                firstText(cardNode, "corp_txt"),
                firstText(cardNode.get("corp"), "name")
        );
        String normalizedCompany = normalizeCompany(company);
        if (normalizedCompany != null) {
            return normalizedCompany;
        }
        return company == null ? null : normalizeText(company);
    }

    // 카드고릴라 API 응답의 대표 카드 이미지 URL을 추출합니다.
    private String extractApiImage(JsonNode cardNode) {
        String image = firstText(cardNode.get("card_img"), "url");
        if (image != null && !image.trim().isEmpty()) {
            return image.trim();
        }
        JsonNode cardImages = cardNode.get("card_imgs");
        if (cardImages != null && cardImages.isArray()) {
            for (JsonNode cardImage : cardImages) {
                image = firstText(cardImage, "url");
                if (image != null && !image.trim().isEmpty()) {
                    return image.trim();
                }
            }
        }
        return null;
    }

    // JSON 객체에서 첫 번째 문자열 필드를 읽습니다.
    private String firstText(JsonNode node, String... fieldNames) {
        if (node == null || node.isNull()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value == null || value.isNull()) {
                continue;
            }
            String text = value.asText();
            if (text != null && !text.trim().isEmpty()) {
                return text.trim();
            }
        }
        return null;
    }

    // JSON 객체에서 첫 번째 숫자 또는 금액 문자열 필드를 읽습니다.
    private Long firstLong(JsonNode node, String... fieldNames) {
        if (node == null || node.isNull()) {
            return null;
        }
        for (String fieldName : fieldNames) {
            JsonNode value = node.get(fieldName);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isNumber()) {
                return Math.max(value.longValue(), 0L);
            }
            if (value.isTextual()) {
                Long amount = toWonAmount(value.asText());
                if (amount != null) {
                    return amount;
                }
                Long digits = parseDigits(value.asText());
                if (digits != null) {
                    return Math.max(digits, 0L);
                }
            }
        }
        return null;
    }

    // JSON 문자열 배열을 List<String>으로 변환합니다.
    private List<String> parseTextArray(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return Collections.emptyList();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode itemNode : arrayNode) {
            if (itemNode == null || itemNode.isNull()) {
                continue;
            }
            String value = itemNode.asText();
            if (value != null && !value.trim().isEmpty()) {
                values.add(value.trim());
            }
        }
        return values;
    }

    // 여러 문자열 중 첫 번째 유효 값을 반환합니다.
    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    // 혜택 설명 후보를 중복 없이 한 문장으로 합칩니다.
    private String joinTexts(String... values) {
        Set<String> texts = new LinkedHashSet<>();
        for (String value : values) {
            String normalizedText = normalizeText(value);
            if (!normalizedText.isEmpty()) {
                texts.add(normalizedText);
            }
        }
        return String.join(" ", texts);
    }

    // 카드고릴라 API의 HTML 혜택 설명을 텍스트로 변환합니다.
    private String htmlToText(String html) {
        if (html == null || html.trim().isEmpty()) {
            return null;
        }
        return normalizeText(Jsoup.parse(html).text());
    }

    // DB에 저장할 설명 길이가 과도하게 커지지 않도록 제한합니다.
    private String limitText(String text, Integer maxLength) {
        if (text == null || maxLength == null || maxLength <= 0 || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    // 셀렉터 후보에서 첫 번째 텍스트 또는 content 속성을 읽습니다.
    private String firstText(Document document, String... selectors) {
        for (String selector : selectors) {
            Element element = document.selectFirst(selector);
            if (element == null) {
                continue;
            }
            String value = element.hasAttr("content") ? element.attr("content") : element.text();
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    // 공백을 한 칸으로 정규화합니다.
    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }
}
