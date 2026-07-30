package com.project.domain.codef;

import com.project.domain.codef.util.CodefRsaUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodefRsaUtilTest {

    @Test
    @DisplayName("CODEF Public Key 기반 RSA 비밀번호 암호화 테스트")
    void encryptPassword_Success() throws Exception {
        // given: 테스트용 RSA KeyPair 생성
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
        keyPairGen.initialize(2048);
        KeyPair keyPair = keyPairGen.generateKeyPair();
        String publicKeyPem = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

        String plainPassword = "testPassword123!";

        // when
        String encryptedPassword = CodefRsaUtil.encryptPassword(plainPassword, publicKeyPem);

        // then
        assertNotNull(encryptedPassword);
        assertTrue(encryptedPassword.length() > 0);
    }
}
