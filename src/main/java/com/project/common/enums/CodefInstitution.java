package com.project.common.enums;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.util.StringUtils;

@Getter
public enum CodefInstitution {

    // 은행 (BK)
    KB_BANK("KB국민은행", "0004", "BK"),
    SHINHAN_BANK("신한은행", "0088", "BK"),
    WOORI_BANK("우리은행", "0020", "BK"),
    HANA_BANK("하나은행", "0081", "BK"),
    NH_BANK("NH농협은행", "0011", "BK"),
    IBK_BANK("IBK기업은행", "0003", "BK"),
    KAKAO_BANK("카카오뱅크", "0090", "BK"),
    KBANK("케이뱅크", "0089", "BK"),
    TOSS_BANK("토스뱅크", "0092", "BK"),

    // 카드 (CD)
    KB_CARD("KB국민카드", "0301", "CD"),
    SHINHAN_CARD("신한카드", "0306", "CD"),
    SAMSUNG_CARD("삼성카드", "0303", "CD"),
    HYUNDAI_CARD("현대카드", "0302", "CD"),
    LOTTE_CARD("롯데카드", "0311", "CD"),
    WOORI_CARD("우리카드", "0309", "CD"),
    HANA_CARD("하나카드", "0313", "CD"),
    NH_CARD("NH농협카드", "0304", "CD");

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
