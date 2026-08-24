package com.project.domain.strategy.card.model;

import com.project.domain.strategy.card.model.CardStrategyRecord.CardProductRecord;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class CardProductMatcher {

    private static final Pattern BRACKET_TEXT_PATTERN = Pattern.compile(
            "\\([^)]*\\)|\\[[^\\]]*\\]|\\{[^}]*\\}|（[^）]*）|［[^］]*］|【[^】]*】|〈[^〉]*〉|《[^》]*》"
    );
    private static final Pattern SUPPLEMENTAL_WORD_PATTERN =
            Pattern.compile("rf|일반형|후불교통|체크|신용|카드", Pattern.CASE_INSENSITIVE);
    private static final Pattern NON_TOKEN_PATTERN = Pattern.compile("[^0-9a-z가-힣]+");
    private static final Integer COMPANY_EXACT_MATCH_SCORE = 100;
    private static final Integer COMPANY_CONTAINS_MATCH_SCORE = 50;
    private static final Integer NAME_EXACT_MATCH_SCORE = 100;
    private static final Integer NAME_CONTAINS_MATCH_SCORE = 50;
    private static final Integer NAME_TOKEN_MATCH_SCORE = 10;
    private static final Integer MAX_LENGTH_SCORE = 20;

    // 사용자 보유 카드명과 가장 유사한 카드 상품을 점수 기반으로 찾습니다.
    public CardProductRecord findBestMatchingProduct(
            String company,
            String cardName,
            List<CardProductRecord> cardProducts
    ) {
        if (cardProducts == null || cardProducts.isEmpty()) {
            return null;
        }

        return cardProducts.stream()
                .map(cardProduct -> MatchCandidate.of(
                        cardProduct,
                        calculateMatchScore(
                                company,
                                cardName,
                                cardProduct.getCompany(),
                                cardProduct.getCardName()
                        )
                ))
                .filter(candidate -> candidate.getScore() > 0)
                .max(matchCandidateComparator())
                .map(MatchCandidate::getCardProduct)
                .orElse(null);
    }

    // 카드사 일치 여부와 카드명 유사도를 합산한 매칭 점수를 계산합니다.
    public Integer calculateMatchScore(
            String company,
            String cardName,
            String productCompany,
            String productName
    ) {
        String companyKey = normalizeCompany(company);
        String productCompanyKey = normalizeCompany(productCompany);
        if (companyKey.isEmpty() || productCompanyKey.isEmpty()) {
            return 0;
        }
        if (!matchesByContains(companyKey, productCompanyKey)) {
            return 0;
        }

        NameProfile cardNameProfile = normalizeCardName(cardName, companyKey, productCompanyKey);
        NameProfile productNameProfile = normalizeCardName(productName, companyKey, productCompanyKey);
        if (cardNameProfile.getCompactName().isEmpty() || productNameProfile.getCompactName().isEmpty()) {
            return 0;
        }

        Integer tokenOverlapCount = countTokenOverlap(cardNameProfile.getTokens(), productNameProfile.getTokens());
        Boolean exactNameMatched = cardNameProfile.getCompactName().equals(productNameProfile.getCompactName());
        Boolean containsNameMatched = matchesByContains(
                cardNameProfile.getCompactName(),
                productNameProfile.getCompactName()
        );
        if (!exactNameMatched && !containsNameMatched && tokenOverlapCount <= 0) {
            return 0;
        }

        Integer score = companyKey.equals(productCompanyKey)
                ? COMPANY_EXACT_MATCH_SCORE
                : COMPANY_CONTAINS_MATCH_SCORE;
        score += tokenOverlapCount * NAME_TOKEN_MATCH_SCORE;
        if (exactNameMatched) {
            score += NAME_EXACT_MATCH_SCORE;
        } else if (containsNameMatched) {
            score += NAME_CONTAINS_MATCH_SCORE;
        }
        score += Math.min(
                Math.min(cardNameProfile.getCompactName().length(), productNameProfile.getCompactName().length()),
                MAX_LENGTH_SCORE
        );
        return score;
    }

    private Comparator<MatchCandidate> matchCandidateComparator() {
        return Comparator
                .comparing(MatchCandidate::getScore)
                .thenComparing(candidate -> safeText(candidate.getCardProduct().getConditionCheckedAt()))
                .thenComparing(candidate -> nullToZero(candidate.getCardProduct().getCardProductId()));
    }

    private Integer countTokenOverlap(Set<String> sourceTokens, Set<String> targetTokens) {
        if (sourceTokens.isEmpty() || targetTokens.isEmpty()) {
            return 0;
        }
        Integer overlapCount = 0;
        for (String token : sourceTokens) {
            if (targetTokens.contains(token)) {
                overlapCount++;
            }
        }
        return overlapCount;
    }

    private Boolean matchesByContains(String firstValue, String secondValue) {
        if (firstValue.isEmpty() || secondValue.isEmpty()) {
            return false;
        }
        return firstValue.equals(secondValue)
                || firstValue.contains(secondValue)
                || secondValue.contains(firstValue);
    }

    private String normalizeCompany(String value) {
        return normalizeWords(value, true)
                .replace(" ", "");
    }

    private NameProfile normalizeCardName(String value, String... companyKeys) {
        String words = normalizeWords(value, false);
        if (words.isEmpty()) {
            return new NameProfile("", Collections.emptySet());
        }

        Set<String> tokens = new LinkedHashSet<>();
        Arrays.stream(words.split(" "))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .filter(token -> isMeaningfulToken(token, companyKeys))
                .forEach(tokens::add);
        return new NameProfile(String.join("", tokens), tokens);
    }

    private String normalizeWords(String value, Boolean companyOnly) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }
        String normalizedValue = Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        if (!companyOnly) {
            normalizedValue = BRACKET_TEXT_PATTERN.matcher(normalizedValue).replaceAll(" ");
        }
        normalizedValue = SUPPLEMENTAL_WORD_PATTERN.matcher(normalizedValue).replaceAll(" ");
        normalizedValue = NON_TOKEN_PATTERN.matcher(normalizedValue).replaceAll(" ");
        return normalizedValue.trim().replaceAll("\\s+", " ");
    }

    private Boolean isMeaningfulToken(String token, String... companyKeys) {
        if (token.length() <= 1) {
            return false;
        }
        for (String companyKey : companyKeys) {
            if (!companyKey.isEmpty() && (companyKey.contains(token) || token.contains(companyKey))) {
                return false;
            }
        }
        return true;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private Long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private static class MatchCandidate {

        private final CardProductRecord cardProduct;
        private final Integer score;

        private MatchCandidate(CardProductRecord cardProduct, Integer score) {
            this.cardProduct = cardProduct;
            this.score = score;
        }

        private static MatchCandidate of(CardProductRecord cardProduct, Integer score) {
            return new MatchCandidate(cardProduct, score);
        }

        private CardProductRecord getCardProduct() {
            return cardProduct;
        }

        private Integer getScore() {
            return score;
        }
    }

    private static class NameProfile {

        private final String compactName;
        private final Set<String> tokens;

        private NameProfile(String compactName, Set<String> tokens) {
            this.compactName = compactName;
            this.tokens = tokens;
        }

        private String getCompactName() {
            return compactName;
        }

        private Set<String> getTokens() {
            return tokens;
        }
    }
}
