package com.project.domain.strategy.card.service.collection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.strategy.card.dto.CardProductCollectRequestDTO;
import com.project.domain.strategy.card.dto.CardProductCollectionResultDTO;
import com.project.domain.strategy.card.mapper.CardStrategyMapper;
import com.project.domain.strategy.card.model.CardStrategyRecord.CardProductSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class CardProductCollectionServiceImpl implements CardProductCollectionService {

    // 수집 실패 항목을 민감정보 없이 기록하기 위한 로거입니다.
    private static final Logger log = LogManager.getLogger(CardProductCollectionServiceImpl.class);

    // 카드 상품 혜택 원문을 JSON 문자열로 정규화하는 파서입니다.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // 카드 상품 저장과 갱신을 담당하는 Mapper입니다.
    private final CardStrategyMapper cardStrategyMapper;

    // 운영 환경에서 사용하는 수집 서비스 생성자입니다.
    public CardProductCollectionServiceImpl(CardStrategyMapper cardStrategyMapper) {
        this.cardStrategyMapper = cardStrategyMapper;
    }

    // 카드 상품 수집 결과를 정규화해 신규 등록 또는 중복 갱신합니다.
    @Override
    public CardProductCollectionResultDTO collectProducts(List<CardProductCollectRequestDTO> products) {
        List<CardProductCollectRequestDTO> safeProducts = products == null ? Collections.emptyList() : products;
        List<String> failedProducts = new ArrayList<>();
        Integer successCount = 0;

        for (CardProductCollectRequestDTO product : safeProducts) {
            try {
                CardProductSource source = normalize(product);
                upsertProduct(source);
                successCount++;
            } catch (Exception exception) {
                String productName = safeProductName(product);
                failedProducts.add(productName);
                log.warn("Card product collection failed. product={}", productName, exception);
            }
        }

        return CardProductCollectionResultDTO.builder()
                .requestedCount(safeProducts.size())
                .successCount(successCount)
                .failureCount(failedProducts.size())
                .failedProducts(failedProducts)
                .build();
    }

    // 수집된 카드 상품 값을 저장 가능한 형태로 정규화합니다.
    private CardProductSource normalize(CardProductCollectRequestDTO product) {
        if (product == null) {
            throw new IllegalArgumentException("카드 상품 데이터가 비어 있습니다.");
        }
        String company = requiredText(product.getCompany(), "카드사");
        String name = requiredText(product.getName(), "카드명");
        Integer annualFee = product.getAnnualFee() == null ? 0 : product.getAnnualFee();
        if (annualFee < 0) {
            throw new IllegalArgumentException("연회비는 0 이상이어야 합니다.");
        }

        return CardProductSource.builder()
                .company(company)
                .name(name)
                .annualFee(annualFee)
                .image(blankToNull(product.getImage()))
                .benefit(normalizeBenefit(product.getBenefit()))
                .conditionText(blankToNull(product.getConditionText()))
                .build();
    }

    // 기존 상품이 있으면 갱신하고 없으면 새로 등록합니다.
    private void upsertProduct(CardProductSource source) {
        Long existingProductId = cardStrategyMapper.selectCardProductIdByCompanyAndName(
                source.getCompany(),
                source.getName()
        );
        if (existingProductId == null) {
            cardStrategyMapper.insertCardProduct(source);
        } else {
            cardStrategyMapper.updateCardProductByCompanyAndName(source);
        }
        cardStrategyMapper.updateUserCardsProductIdByCompanyAndName(source);
    }

    // 혜택 원문을 JSON 컬럼에 저장 가능한 문자열로 정규화합니다.
    private String normalizeBenefit(String benefit) {
        if (benefit == null || benefit.trim().isEmpty()) {
            return "[]";
        }

        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(benefit);
            return OBJECT_MAPPER.writeValueAsString(jsonNode);
        } catch (Exception exception) {
            try {
                return OBJECT_MAPPER.writeValueAsString(Collections.singletonList(benefit.trim()));
            } catch (Exception jsonException) {
                throw new IllegalArgumentException("혜택 정보를 JSON으로 정규화할 수 없습니다.");
            }
        }
    }

    // 필수 문자열 값을 공백 제거 후 검증합니다.
    private String requiredText(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은 필수입니다.");
        }
        return value.trim();
    }

    // 공백 문자열을 null로 변환해 DB 기본값과 선택 컬럼 의미를 유지합니다.
    private String blankToNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    // 실패 로그와 결과에 사용할 안전한 상품명을 구성합니다.
    private String safeProductName(CardProductCollectRequestDTO product) {
        if (product == null) {
            return "UNKNOWN";
        }
        String company = blankToNull(product.getCompany());
        String name = blankToNull(product.getName());
        if (company == null && name == null) {
            return "UNKNOWN";
        }
        if (company == null) {
            return name;
        }
        if (name == null) {
            return company;
        }
        return company + " " + name;
    }
}
