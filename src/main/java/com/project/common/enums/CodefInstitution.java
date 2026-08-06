package com.project.common.enums;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
public enum CodefInstitution {

    // 은행 (BK)
    KDB_BANK("산업은행", "0002", "BK"),
    IBK_BANK("기업은행", "0003", "BK"),
    KB_BANK("국민은행", "0004", "BK"),
    SUHYUP_BANK("수협은행", "0007", "BK"),
    NH_BANK("농협은행", "0011", "BK"),
    WOORI_BANK("우리은행", "0020", "BK"),
    SC_BANK("SC은행", "0023", "BK"),
    CITI_BANK("씨티은행", "0027", "BK"),
    DAEGU_BANK("대구은행", "0031", "BK"),
    BUSAN_BANK("부산은행", "0032", "BK"),
    KWANGJU_BANK("광주은행", "0034", "BK"),
    JEJU_BANK("제주은행", "0035", "BK"),
    JEONBUK_BANK("전북은행", "0037", "BK"),
    KYONGNAM_BANK("경남은행", "0039", "BK"),
    KFCC("새마을금고", "0045", "BK"),
    CU("신협은행", "0048", "BK"),
    POST("우체국", "0071", "BK"),
    HANA_BANK("KEB하나은행", "0081", "BK"),
    SHINHAN_BANK("신한은행", "0088", "BK"),
    KBANK("K뱅크", "0089", "BK"),

    // 카드 (CD)
    KB_CARD("KB카드", "0301", "CD"),
    HYUNDAI_CARD("현대카드", "0302", "CD"),
    SAMSUNG_CARD("삼성카드", "0303", "CD"),
    NH_CARD("NH카드", "0304", "CD"),
    BC_CARD("BC카드", "0305", "CD"),
    SHINHAN_CARD("신한카드", "0306", "CD"),
    CITI_CARD("씨티카드", "0307", "CD"),
    WOORI_CARD("우리카드", "0309", "CD"),
    LOTTE_CARD("롯데카드", "0311", "CD"),
    HANA_CARD("하나카드", "0313", "CD"),
    JEONBUK_CARD("전북카드", "0315", "CD"),
    KWANGJU_CARD("광주카드", "0316", "CD"),
    SUHYUP_CARD("수협카드", "0320", "CD"),
    JEJU_CARD("제주카드", "0321", "CD");

    private final String officialName;
    private final String organizationCode;
    private final String businessType;

    CodefInstitution(String officialName, String organizationCode, String businessType) {
        this.officialName = officialName;
        this.organizationCode = organizationCode;
        this.businessType = businessType;
    }

    public static CodefInstitution findByCompany(String inputCompany) {
        if (!StringUtils.hasText(inputCompany)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "금융기관 이름(company) 또는 기관 코드를 입력해주세요.");
        }
        String cleanInput = inputCompany.trim();

        for (CodefInstitution inst : values()) {
            if (inst.organizationCode.equalsIgnoreCase(cleanInput) || inst.officialName.equalsIgnoreCase(cleanInput)) {
                return inst;
            }
        }
        throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "지원되지 않거나 찾을 수 없는 금융기관입니다: " + inputCompany);
    }
}
