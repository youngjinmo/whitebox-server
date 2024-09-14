package io.andy.shorten_url.util.encrypt;

import io.andy.shorten_url.exception.server.InternalServerException;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

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

            // 해싱 결과를 Base64로 인코딩하여 반환
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("failed to encrypt word by {}", e.getMessage());
            throw new InternalServerException("FAILED TO ENCRYPT");
        }
    }
}
