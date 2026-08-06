package com.project.common.enums;

import com.project.common.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CodefInstitutionTest {

    @Test
    @DisplayName("20개 은행 데이터가 CodefInstitution에 정상 반영되었는지 확인")
    void checkTwentyBanksExist() {
        assertEquals("0002", CodefInstitution.findByCompany("산업은행").getOrganizationCode());
        assertEquals("0003", CodefInstitution.findByCompany("기업은행").getOrganizationCode());
        assertEquals("0004", CodefInstitution.findByCompany("국민은행").getOrganizationCode());
        assertEquals("0007", CodefInstitution.findByCompany("수협은행").getOrganizationCode());
        assertEquals("0011", CodefInstitution.findByCompany("농협은행").getOrganizationCode());
        assertEquals("0020", CodefInstitution.findByCompany("우리은행").getOrganizationCode());
        assertEquals("0023", CodefInstitution.findByCompany("SC은행").getOrganizationCode());
        assertEquals("0027", CodefInstitution.findByCompany("씨티은행").getOrganizationCode());
        assertEquals("0031", CodefInstitution.findByCompany("대구은행").getOrganizationCode());
        assertEquals("0032", CodefInstitution.findByCompany("부산은행").getOrganizationCode());
        assertEquals("0034", CodefInstitution.findByCompany("광주은행").getOrganizationCode());
        assertEquals("0035", CodefInstitution.findByCompany("제주은행").getOrganizationCode());
        assertEquals("0037", CodefInstitution.findByCompany("전북은행").getOrganizationCode());
        assertEquals("0039", CodefInstitution.findByCompany("경남은행").getOrganizationCode());
        assertEquals("0045", CodefInstitution.findByCompany("새마을금고").getOrganizationCode());
        assertEquals("0048", CodefInstitution.findByCompany("신협은행").getOrganizationCode());
        assertEquals("0071", CodefInstitution.findByCompany("우체국").getOrganizationCode());
        assertEquals("0081", CodefInstitution.findByCompany("KEB하나은행").getOrganizationCode());
        assertEquals("0088", CodefInstitution.findByCompany("신한은행").getOrganizationCode());
        assertEquals("0089", CodefInstitution.findByCompany("K뱅크").getOrganizationCode());
    }

    @Test
    @DisplayName("기관 코드로 조회 시 적절한 CodefInstitution을 반환해야 함")
    void findByCompanyWithOrganizationCode() {
        assertEquals(CodefInstitution.KDB_BANK, CodefInstitution.findByCompany("0002"));
        assertEquals(CodefInstitution.IBK_BANK, CodefInstitution.findByCompany("0003"));
        assertEquals(CodefInstitution.KB_BANK, CodefInstitution.findByCompany("0004"));
        assertEquals(CodefInstitution.SUHYUP_BANK, CodefInstitution.findByCompany("0007"));
        assertEquals(CodefInstitution.NH_BANK, CodefInstitution.findByCompany("0011"));
        assertEquals(CodefInstitution.WOORI_BANK, CodefInstitution.findByCompany("0020"));
        assertEquals(CodefInstitution.SC_BANK, CodefInstitution.findByCompany("0023"));
        assertEquals(CodefInstitution.CITI_BANK, CodefInstitution.findByCompany("0027"));
        assertEquals(CodefInstitution.DAEGU_BANK, CodefInstitution.findByCompany("0031"));
        assertEquals(CodefInstitution.BUSAN_BANK, CodefInstitution.findByCompany("0032"));
        assertEquals(CodefInstitution.KWANGJU_BANK, CodefInstitution.findByCompany("0034"));
        assertEquals(CodefInstitution.JEJU_BANK, CodefInstitution.findByCompany("0035"));
        assertEquals(CodefInstitution.JEONBUK_BANK, CodefInstitution.findByCompany("0037"));
        assertEquals(CodefInstitution.KYONGNAM_BANK, CodefInstitution.findByCompany("0039"));
        assertEquals(CodefInstitution.KFCC, CodefInstitution.findByCompany("0045"));
        assertEquals(CodefInstitution.CU, CodefInstitution.findByCompany("0048"));
        assertEquals(CodefInstitution.POST, CodefInstitution.findByCompany("0071"));
        assertEquals(CodefInstitution.HANA_BANK, CodefInstitution.findByCompany("0081"));
        assertEquals(CodefInstitution.SHINHAN_BANK, CodefInstitution.findByCompany("0088"));
        assertEquals(CodefInstitution.KBANK, CodefInstitution.findByCompany("0089"));
    }

    @Test
    @DisplayName("14개 카드사 데이터가 CodefInstitution에 정상 반영되었는지 확인")
    void checkFourteenCardsExist() {
        assertEquals("0301", CodefInstitution.findByCompany("KB카드").getOrganizationCode());
        assertEquals("0302", CodefInstitution.findByCompany("현대카드").getOrganizationCode());
        assertEquals("0303", CodefInstitution.findByCompany("삼성카드").getOrganizationCode());
        assertEquals("0304", CodefInstitution.findByCompany("NH카드").getOrganizationCode());
        assertEquals("0305", CodefInstitution.findByCompany("BC카드").getOrganizationCode());
        assertEquals("0306", CodefInstitution.findByCompany("신한카드").getOrganizationCode());
        assertEquals("0307", CodefInstitution.findByCompany("씨티카드").getOrganizationCode());
        assertEquals("0309", CodefInstitution.findByCompany("우리카드").getOrganizationCode());
        assertEquals("0311", CodefInstitution.findByCompany("롯데카드").getOrganizationCode());
        assertEquals("0313", CodefInstitution.findByCompany("하나카드").getOrganizationCode());
        assertEquals("0315", CodefInstitution.findByCompany("전북카드").getOrganizationCode());
        assertEquals("0316", CodefInstitution.findByCompany("광주카드").getOrganizationCode());
        assertEquals("0320", CodefInstitution.findByCompany("수협카드").getOrganizationCode());
        assertEquals("0321", CodefInstitution.findByCompany("제주카드").getOrganizationCode());
    }

    @Test
    @DisplayName("제거된 은행(카카오뱅크 0090, 토스뱅크 0092) 조회 시 CustomException 예외 발생")
    void removedBanksShouldThrowException() {
        assertThrows(CustomException.class, () -> CodefInstitution.findByCompany("카카오뱅크"));
        assertThrows(CustomException.class, () -> CodefInstitution.findByCompany("0090"));
        assertThrows(CustomException.class, () -> CodefInstitution.findByCompany("토스뱅크"));
        assertThrows(CustomException.class, () -> CodefInstitution.findByCompany("0092"));
    }
}
