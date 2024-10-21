package io.andy.shorten_url.util.encrypt;

import io.andy.shorten_url.exception.server.InternalServerException;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
public final class EncodeUtil {
    private EncodeUtil() {}

    private static final String HASH_ALGORITHM = "SHA-256";

    public static String encrypt(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Length must not be null or empty");
        }
        try {
            // 해시 알고리즘 사용하여 MessageDigest 객체 생성
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);

            // 인자를 byte 배열로 변환 후 해싱
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // 해싱 결과를 문자열로 변환 후 반환
            StringBuilder hexString = new StringBuilder();
            for(byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                } else {
                    hexString.append(hex);
                }
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("failed to encrypt word by {}", e.getMessage());
            throw new InternalServerException("FAILED TO ENCRYPT");
        }
    }
}
