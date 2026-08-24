package com.project.domain.strategy.card.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class CardBenefitCatalog {

    // JSON 문자열을 혜택 규칙으로 바꾸는 Jackson 파서입니다.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 문자열 혜택 설명에서 퍼센트 혜택률을 찾는 패턴입니다.
    private static final Pattern PERCENT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*%");

    // 조건 문구에서 원 단위 또는 만 원 단위 금액을 찾는 패턴입니다.
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(\\d[\\d,]*)(\\s*만)?\\s*원");

    // 사용처를 몰라도 적용 가능한 혜택 범위를 판단할 때 사용하는 카테고리 코드입니다.
    private static final Set<String> UNIVERSAL_CATEGORY_CODES = Set.of(
            "ALL",
            "ANY",
            "BASIC",
            "COMMON",
            "DEFAULT",
            "GENERAL",
            "UNIVERSAL"
    );

    // 카테고리가 명시되지 않은 원문에서 전 가맹점·기본 혜택 여부를 판단하는 키워드입니다.
    private static final Set<String> UNIVERSAL_EXACT_KEYWORDS = Set.of(
            "기본",
            "기본혜택",
            "기본할인",
            "기본적립",
            "공통",
            "공통혜택",
            "공통할인",
            "공통적립"
    );

    private CardBenefitCatalog() {
    }

    // 카드 상품의 혜택 JSON과 조건 문구를 계산 가능한 혜택 규칙 목록으로 변환합니다.
    public static List<CardBenefitRule> parse(String benefitJson, String conditionText) {
        List<Long> conditionThresholds = parseAmounts(conditionText);
        if (benefitJson == null || benefitJson.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            JsonNode rootNode = OBJECT_MAPPER.readTree(benefitJson);
            if (rootNode.isArray()) {
                return parseArray(rootNode, null, conditionThresholds);
            }
            if (rootNode.isObject()) {
                JsonNode benefitItems = firstArray(rootNode, "benefits", "items", "rules");
                if (benefitItems != null) {
                    return parseArray(benefitItems, rootNode, conditionThresholds);
                }
                return Collections.singletonList(parseObject(rootNode, null, conditionThresholds));
            }
            if (rootNode.isTextual()) {
                return Collections.singletonList(parseText(rootNode.asText(), conditionThresholds));
            }
        } catch (Exception exception) {
            return parseFallbackText(benefitJson, conditionThresholds);
        }

        return Collections.emptyList();
    }

    // 계산 가능한 혜택 값이 하나라도 있는지 확인합니다.
    public static Boolean hasCalculableBenefit(List<CardBenefitRule> rules) {
        if (rules == null) {
            return false;
        }
        return rules.stream()
                .anyMatch(rule -> rule != null
                        && !Boolean.TRUE.equals(rule.getExcluded())
                        && Boolean.TRUE.equals(rule.hasBenefitValue()));
    }

    // 사용처 입력 없이도 확정 계산할 수 있는 전 가맹점·기본 혜택 규칙만 반환합니다.
    public static List<CardBenefitRule> filterAmountOnlyApplicableRules(List<CardBenefitRule> rules) {
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }
        return rules.stream()
                .filter(CardBenefitCatalog::isAmountOnlyApplicable)
                .collect(Collectors.toList());
    }

    // 선택 카테고리에서 적용 가능한 혜택 규칙만 반환합니다. 전 가맹점·기본 혜택은 모든 카테고리에 포함합니다.
    public static List<CardBenefitRule> filterCategoryApplicableRules(
            List<CardBenefitRule> rules,
            CardRecommendationCategory category
    ) {
        if (category == null) {
            return filterAmountOnlyApplicableRules(rules);
        }
        if (rules == null || rules.isEmpty()) {
            return Collections.emptyList();
        }
        return rules.stream()
                .filter(rule -> isCategoryApplicable(rule, category))
                .collect(Collectors.toList());
    }

    // 사용처 입력 없이도 해당 혜택을 확정 계산할 수 있는지 확인합니다.
    public static Boolean isAmountOnlyApplicable(CardBenefitRule rule) {
        if (rule == null || Boolean.TRUE.equals(rule.getExcluded()) || !Boolean.TRUE.equals(rule.hasBenefitValue())) {
            return false;
        }
        List<String> categories = rule.safeCategories();
        if (!categories.isEmpty()) {
            return categories.stream().anyMatch(CardBenefitCatalog::isUniversalScopeText);
        }
        return hasUniversalKeyword(rule.getTitle()) || hasUniversalKeyword(rule.getDescription());
    }

    // 선택 카테고리에서 해당 혜택을 계산 대상으로 볼 수 있는지 확인합니다.
    public static Boolean isCategoryApplicable(CardBenefitRule rule, CardRecommendationCategory category) {
        if (category == null) {
            return isAmountOnlyApplicable(rule);
        }
        if (rule == null || Boolean.TRUE.equals(rule.getExcluded()) || !Boolean.TRUE.equals(rule.hasBenefitValue())) {
            return false;
        }
        if (Boolean.TRUE.equals(isAmountOnlyApplicable(rule))) {
            return true;
        }

        Boolean categoryMatched = rule.safeCategories().stream()
                .anyMatch(category::matchesBenefitScope);
        return categoryMatched
                || category.matchesBenefitScope(rule.getTitle())
                || category.matchesBenefitScope(rule.getDescription());
    }

    // 배열 형태의 혜택 항목을 규칙 목록으로 변환합니다.
    private static List<CardBenefitRule> parseArray(JsonNode arrayNode, JsonNode defaultNode, List<Long> conditionThresholds) {
        List<CardBenefitRule> rules = new ArrayList<>();
        for (JsonNode itemNode : arrayNode) {
            if (itemNode.isObject()) {
                rules.add(parseObject(itemNode, defaultNode, conditionThresholds));
            } else if (itemNode.isTextual()) {
                rules.add(parseText(itemNode.asText(), conditionThresholds));
            }
        }
        return rules;
    }

    // 객체 형태의 혜택 항목을 단일 규칙으로 변환합니다.
    private static CardBenefitRule parseObject(JsonNode node, JsonNode defaultNode, List<Long> conditionThresholds) {
        String title = firstText(node, "title", "name", "benefitName", "benefit_name", "category", "categoryName");
        String description = firstText(node, "description", "desc", "benefitDescription", "benefit_description", "summary");
        BigDecimal benefitRate = normalizeRate(firstDecimal(node, defaultNode,
                "benefitRate", "benefit_rate", "discountRate", "discount_rate", "cashbackRate", "rate", "value"));
        Long fixedBenefitAmount = firstLong(node, defaultNode,
                "fixedBenefitAmount", "fixed_benefit_amount", "fixedAmount", "fixed_amount");
        Long monthlyLimitAmount = firstLong(node, defaultNode,
                "monthlyLimitAmount", "monthly_limit_amount", "monthlyLimit", "monthly_limit", "limit", "monthlyBenefitLimit");
        Long minimumPerformanceAmount = firstLong(node, defaultNode,
                "minimumPerformanceAmount", "minimum_performance_amount", "minimumMonthlySpend", "minimum_monthly_spend",
                "requiredMonthlySpend", "required_monthly_spend", "minSpend", "performanceThreshold");
        List<String> categories = parseCategories(node);
        List<Long> benefitThresholds = mergeThresholds(conditionThresholds, parseThresholds(node, defaultNode));
        Boolean excluded = firstBoolean(node, "excluded", "exclude", "isExcluded", "is_excluded");
        List<String> excludedKeywords = parseTextArray(node, "excludedKeywords", "excluded_keywords", "excludedCategories",
                "excluded_categories");

        return CardBenefitRule.builder()
                .title(emptyToDefault(title, "기본 혜택"))
                .description(description)
                .benefitRate(benefitRate)
                .fixedBenefitAmount(fixedBenefitAmount)
                .monthlyLimitAmount(monthlyLimitAmount)
                .minimumPerformanceAmount(minimumPerformanceAmount)
                .categories(categories)
                .benefitThresholds(benefitThresholds)
                .excludedKeywords(excludedKeywords)
                .excluded(excluded)
                .build();
    }

    // 텍스트 혜택 설명에서 최소한의 계산 규칙을 추출합니다.
    private static CardBenefitRule parseText(String text, List<Long> conditionThresholds) {
        return CardBenefitRule.builder()
                .title(emptyToDefault(text, "기본 혜택"))
                .description(text)
                .benefitRate(parsePercentRate(text))
                .categories(Collections.emptyList())
                .benefitThresholds(conditionThresholds)
                .build();
    }

    // JSON 파싱 실패 시 원문 텍스트에서 퍼센트와 조건 금액만 추출합니다.
    private static List<CardBenefitRule> parseFallbackText(String text, List<Long> conditionThresholds) {
        if (parsePercentRate(text) == null) {
            return Collections.emptyList();
        }
        return Collections.singletonList(parseText(text, conditionThresholds));
    }

    // 객체에서 첫 번째 배열 필드를 찾습니다.
    private static JsonNode firstArray(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && value.isArray()) {
                return value;
            }
        }
        return null;
    }

    // 객체에서 첫 번째 문자열 필드를 찾습니다.
    private static String firstText(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) {
                return value.asText();
            }
        }
        return null;
    }

    // 항목 값이 없을 때 기본 객체에서 첫 번째 숫자 필드를 찾습니다.
    private static BigDecimal firstDecimal(JsonNode node, JsonNode defaultNode, String... names) {
        BigDecimal value = firstDecimal(node, names);
        if (value != null || defaultNode == null) {
            return value;
        }
        return firstDecimal(defaultNode, names);
    }

    // 객체에서 첫 번째 숫자 필드를 BigDecimal로 변환합니다.
    private static BigDecimal firstDecimal(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isNumber()) {
                return value.decimalValue();
            }
            if (value.isTextual()) {
                try {
                    String textValue = value.asText().trim();
                    if (textValue.contains("%")) {
                        return parsePercentRate(textValue);
                    }
                    return new BigDecimal(textValue);
                } catch (NumberFormatException ignored) {
                    return parsePercentRate(value.asText());
                }
            }
        }
        return null;
    }

    // 항목 값이 없을 때 기본 객체에서 첫 번째 금액 필드를 찾습니다.
    private static Long firstLong(JsonNode node, JsonNode defaultNode, String... names) {
        Long value = firstLong(node, names);
        if (value != null || defaultNode == null) {
            return value;
        }
        return firstLong(defaultNode, names);
    }

    // 객체에서 첫 번째 금액 필드를 Long으로 변환합니다.
    private static Long firstLong(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isNumber()) {
                return Math.max(value.longValue(), 0L);
            }
            if (value.isTextual()) {
                Long parsedValue = parseFirstAmount(value.asText());
                if (parsedValue != null) {
                    return parsedValue;
                }
            }
        }
        return null;
    }

    // 객체에서 첫 번째 불리언 필드를 찾습니다.
    private static Boolean firstBoolean(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) {
                return value.asBoolean(false);
            }
        }
        return false;
    }

    // 객체의 배열 필드를 문자열 목록으로 변환합니다.
    private static List<String> parseTextArray(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value == null || !value.isArray()) {
                continue;
            }
            List<String> result = new ArrayList<>();
            for (JsonNode item : value) {
                if (!item.isNull()) {
                    result.add(item.asText());
                }
            }
            return result;
        }
        return Collections.emptyList();
    }

    // 객체에서 적용 카테고리 배열 또는 단일 카테고리 값을 추출합니다.
    private static List<String> parseCategories(JsonNode node) {
        List<String> categories = parseTextArray(node,
                "categories", "categoryCodes", "category_codes", "benefitCategories", "benefit_categories",
                "targetCategories", "target_categories");
        if (!categories.isEmpty()) {
            return normalizeCategories(categories);
        }

        String category = firstText(node,
                "category", "categoryName", "category_name", "categoryCode", "category_code",
                "targetCategory", "target_category");
        if (category == null || category.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return normalizeCategories(Collections.singletonList(category));
    }

    // 카테고리 값의 공백을 제거하고 빈 값을 제외합니다.
    private static List<String> normalizeCategories(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return Collections.emptyList();
        }
        return categories.stream()
                .filter(value -> value != null && !value.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.toList());
    }

    // JSON의 혜택 구간 필드와 최소 실적 필드를 금액 목록으로 변환합니다.
    private static List<Long> parseThresholds(JsonNode node, JsonNode defaultNode) {
        List<Long> thresholds = new ArrayList<>();
        thresholds.addAll(parseLongArray(firstArray(node, "benefitThresholds", "benefit_thresholds", "thresholds")));
        Long minimumPerformanceAmount = firstLong(node, defaultNode,
                "minimumPerformanceAmount", "minimum_performance_amount", "minimumMonthlySpend", "minimum_monthly_spend",
                "requiredMonthlySpend", "required_monthly_spend", "minSpend", "performanceThreshold");
        if (minimumPerformanceAmount != null) {
            thresholds.add(minimumPerformanceAmount);
        }
        if (defaultNode != null) {
            thresholds.addAll(parseLongArray(firstArray(defaultNode, "benefitThresholds", "benefit_thresholds", "thresholds")));
        }
        return thresholds;
    }

    // JSON 배열의 숫자 또는 금액 문자열을 원 단위 목록으로 변환합니다.
    private static List<Long> parseLongArray(JsonNode arrayNode) {
        if (arrayNode == null) {
            return Collections.emptyList();
        }
        List<Long> values = new ArrayList<>();
        for (JsonNode item : arrayNode) {
            if (item.isNumber()) {
                values.add(Math.max(item.longValue(), 0L));
            } else if (item.isTextual()) {
                Long amount = parseFirstAmount(item.asText());
                if (amount != null) {
                    values.add(amount);
                }
            }
        }
        return values;
    }

    // 두 금액 목록을 중복 없이 오름차순으로 합칩니다.
    private static List<Long> mergeThresholds(List<Long> firstValues, List<Long> secondValues) {
        Set<Long> mergedValues = new LinkedHashSet<>();
        if (firstValues != null) {
            mergedValues.addAll(firstValues);
        }
        if (secondValues != null) {
            mergedValues.addAll(secondValues);
        }
        return mergedValues.stream()
                .filter(value -> value != null && value > 0)
                .sorted()
                .toList();
    }

    // 조건 문구에서 발견한 모든 금액을 원 단위 목록으로 변환합니다.
    private static List<Long> parseAmounts(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> amounts = new ArrayList<>();
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        while (matcher.find()) {
            amounts.add(toWonAmount(matcher.group(1), matcher.group(2) != null));
        }
        return amounts;
    }

    // 문자열에서 첫 번째 금액을 원 단위로 변환합니다.
    private static Long parseFirstAmount(String text) {
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        if (!matcher.find()) {
            try {
                return Long.valueOf(text.replace(",", "").trim());
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return toWonAmount(matcher.group(1), matcher.group(2) != null);
    }

    // 금액 문자열과 만 원 여부를 원 단위 Long으로 변환합니다.
    private static Long toWonAmount(String amountText, Boolean tenThousandWonUnit) {
        Long amount = Long.valueOf(amountText.replace(",", ""));
        if (Boolean.TRUE.equals(tenThousandWonUnit)) {
            return amount * 10_000L;
        }
        return amount;
    }

    // 숫자 형태의 혜택률을 소수 비율로 정규화합니다.
    private static BigDecimal normalizeRate(BigDecimal rawRate) {
        if (rawRate == null) {
            return null;
        }
        if (rawRate.compareTo(BigDecimal.ONE) > 0) {
            return rawRate.divide(BigDecimal.valueOf(100L), 6, RoundingMode.HALF_UP);
        }
        return rawRate;
    }

    // 문자열에서 퍼센트 혜택률을 소수 비율로 변환합니다.
    private static BigDecimal parsePercentRate(String text) {
        BigDecimal rate = parsePercentAsNumber(text);
        if (rate == null) {
            return null;
        }
        return rate.divide(BigDecimal.valueOf(100L), 6, RoundingMode.HALF_UP);
    }

    // 문자열에서 퍼센트 숫자만 BigDecimal로 추출합니다.
    private static BigDecimal parsePercentAsNumber(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = PERCENT_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return new BigDecimal(matcher.group(1));
    }

    // 빈 문자열일 때 기본 문구를 사용합니다.
    private static String emptyToDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    // 텍스트가 전 가맹점·기본 혜택 범위를 의미하는지 판단합니다.
    private static Boolean isUniversalScopeText(String text) {
        String normalizedText = normalizeScopeText(text);
        return UNIVERSAL_CATEGORY_CODES.contains(normalizedText)
                || UNIVERSAL_EXACT_KEYWORDS.contains(normalizedText)
                || normalizedText.contains("전가맹점")
                || normalizedText.contains("전체가맹점")
                || normalizedText.contains("모든가맹점")
                || normalizedText.contains("국내외가맹점")
                || normalizedText.contains("국내전가맹점")
                || normalizedText.contains("가맹점전체");
    }

    // 카테고리가 없는 원문에서 전 가맹점·기본 혜택 키워드를 찾습니다.
    private static Boolean hasUniversalKeyword(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        return isUniversalScopeText(text);
    }

    // 한글과 코드 값을 동일 기준으로 비교하기 위해 범위 문구를 정규화합니다.
    private static String normalizeScopeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[\\s_\\-·/]", "")
                .toUpperCase(Locale.ROOT);
    }
}
