package com.project.common.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SensitiveDataMaskerTest {

    @Test
    void masksJwtPhoneAccountAndSensitiveKeyValues() {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.signature";
        String message = "authorization=Bearer " + jwt
                + ", phone=010-9900-0015"
                + ", connectedId: connected-before-001"
                + ", account: 123-456-789012"
                + ", password=BeforeOnly!9001"
                + ", fcmToken=fcm-before-token";

        String masked = SensitiveDataMasker.mask(message);

        assertFalse(masked.contains(jwt));
        assertFalse(masked.contains("010-9900-0015"));
        assertFalse(masked.contains("connected-before-001"));
        assertFalse(masked.contains("123-456-789012"));
        assertFalse(masked.contains("BeforeOnly!9001"));
        assertFalse(masked.contains("fcm-before-token"));
        assertTrue(masked.contains("[REDACTED_JWT]"));
        assertTrue(masked.contains("010-****-0015"));
        assertTrue(masked.contains("account:1234****9012"));
    }

    @Test
    void leavesOrdinaryLogMessageUnchanged() {
        String message = "사용자 거래 동기화 완료 - userId: 15, saved: 20";

        assertEquals(message, SensitiveDataMasker.mask(message));
    }

    @Test
    void handlesNullAndEmptyMessages() {
        assertNull(SensitiveDataMasker.mask(null));
        assertEquals("", SensitiveDataMasker.mask(""));
    }
}
