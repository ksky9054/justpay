package ru.eridian.justpay.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Проверка хеширования паролей */
public class PasswordHasherTest {
    private static final int ITERATIONS = 1000; // для теста хватит, приложению нужно больше

    @Test
    public void correctPassword_matches() {
        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash("secret123", salt, ITERATIONS);
        assertTrue(PasswordHasher.matches("secret123", salt, ITERATIONS, hash));
    }

    @Test
    public void wrongPassword_doesNotMatch() {
        String salt = PasswordHasher.newSalt();
        String hash = PasswordHasher.hash("secret123", salt, ITERATIONS);
        assertFalse(PasswordHasher.matches("secret124", salt, ITERATIONS, hash));
    }

    @Test
    public void samePassword_differentSalt_differentHash() {
        String hashOne = PasswordHasher.hash("secret123", PasswordHasher.newSalt(), ITERATIONS);
        String hashTwo = PasswordHasher.hash("secret123", PasswordHasher.newSalt(), ITERATIONS);
        assertNotEquals(hashOne, hashTwo);
    }

    @Test
    public void hash_neverContainsPassword() {
        String hash = PasswordHasher.hash("secret123", PasswordHasher.newSalt(), ITERATIONS);
        assertFalse(hash.contains("secret123"));
        assertEquals(44, hash.length()); // 32 байта в Base64
    }
}
