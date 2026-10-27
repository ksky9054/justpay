package ru.eridian.justpay.data;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Хеширование паролей: PBKDF2 с HMAC-SHA256, своя случайная соль у каждого аккаунта.
 * Сам пароль нигде не хранится, в базе лежат только соль и хеш.
 */
public final class PasswordHasher {
    private PasswordHasher() {}

    public static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    public static String newSalt() {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public static String hash(String password, String saltBase64, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(),
                Base64.getDecoder().decode(saltBase64), iterations, KEY_BITS);
        try {
            byte[] key = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(key);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Хеширование пароля недоступно", e);
        } finally {
            spec.clearPassword();
        }
    }

    /** Сравнение за постоянное время, чтобы по скорости ответа нельзя было угадывать хеш. */
    public static boolean matches(String password, String saltBase64, int iterations, String expectedHash) {
        byte[] actual = hash(password, saltBase64, iterations).getBytes();
        return MessageDigest.isEqual(actual, expectedHash.getBytes());
    }
}
