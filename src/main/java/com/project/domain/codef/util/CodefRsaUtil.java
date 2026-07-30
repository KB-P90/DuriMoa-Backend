package com.project.domain.codef.util;

import com.project.domain.codef.exception.CodefApiException;
import lombok.extern.log4j.Log4j2;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Log4j2
public class CodefRsaUtil {

    private CodefRsaUtil() {
        // 유틸리티 클래스는 인스턴스화를 방지합니다.
    }

    /**
     * CODEF Public Key를 사용하여 평문 비밀번호를 RSA 암호화합니다.
     *
     * @param plainPassword 평문 비밀번호
     * @param publicKeyPem Base64 인코딩된 CODEF Public Key
     * @return Base64 인코딩된 RSA 암호문
     */
    public static String encryptPassword(String plainPassword, String publicKeyPem) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new CodefApiException("암호화할 비밀번호가 비어 있습니다.");
        }
        if (publicKeyPem == null || publicKeyPem.isEmpty()) {
            throw new CodefApiException("CODEF Public Key가 지정되지 않았습니다.");
        }

        try {
            // PEM 헤더/푸터 및 줄바꿈 공백 제거
            String cleanKey = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] decodedKey = Base64.getDecoder().decode(cleanKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);

            byte[] encryptedBytes = cipher.doFinal(plainPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("RSA 비밀번호 암호화 중 오류 발생", e);
            throw new CodefApiException("비밀번호 RSA 암호화 실패: " + e.getMessage(), e);
        }
    }
}
