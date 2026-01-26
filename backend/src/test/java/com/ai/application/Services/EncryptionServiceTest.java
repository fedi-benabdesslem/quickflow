package com.ai.application.Services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EncryptionService.
 * 
 * Tests cover:
 * - Encrypt/decrypt round-trip with various inputs
 * - Null and empty input handling
 * - Different key lengths
 * - Cipher integrity (encrypted value differs from original)
 * 
 * Not covered:
 * - Cryptographic security analysis (beyond scope of unit tests)
 * - Performance benchmarks
 */
class EncryptionServiceTest {

    private EncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        // Use a test encryption key
        encryptionService = new EncryptionService("TestEncryptionKeyForUnitTests32!");
    }

    @Nested
    @DisplayName("encrypt() method")
    class EncryptTests {

        @Test
        @DisplayName("Should return null for null input")
        void encryptNullInput() {
            assertNull(encryptionService.encrypt(null));
        }

        @Test
        @DisplayName("Should return null for empty input")
        void encryptEmptyInput() {
            assertNull(encryptionService.encrypt(""));
        }

        @Test
        @DisplayName("Should return different ciphertext than plaintext")
        void encryptProducesDifferentOutput() {
            String plaintext = "my-secret-token-12345";
            String ciphertext = encryptionService.encrypt(plaintext);
            
            assertNotNull(ciphertext);
            assertNotEquals(plaintext, ciphertext);
        }

        @Test
        @DisplayName("Should produce Base64 encoded output")
        void encryptProducesBase64Output() {
            String plaintext = "my-secret-token";
            String ciphertext = encryptionService.encrypt(plaintext);
            
            assertNotNull(ciphertext);
            // Base64 should not contain whitespace or unusual characters
            assertTrue(ciphertext.matches("^[A-Za-z0-9+/=]+$"), 
                "Ciphertext should be valid Base64");
        }

        @Test
        @DisplayName("Should produce different ciphertexts for same plaintext (due to random IV)")
        void encryptProducesDifferentCiphertextsForSameInput() {
            String plaintext = "my-secret-token";
            String ciphertext1 = encryptionService.encrypt(plaintext);
            String ciphertext2 = encryptionService.encrypt(plaintext);
            
            // Due to random IV, same plaintext should produce different ciphertexts
            assertNotEquals(ciphertext1, ciphertext2);
        }
    }

    @Nested
    @DisplayName("decrypt() method")
    class DecryptTests {

        @Test
        @DisplayName("Should return null for null input")
        void decryptNullInput() {
            assertNull(encryptionService.decrypt(null));
        }

        @Test
        @DisplayName("Should return null for empty input")
        void decryptEmptyInput() {
            assertNull(encryptionService.decrypt(""));
        }

        @Test
        @DisplayName("Should throw exception for invalid Base64")
        void decryptInvalidBase64() {
            assertThrows(RuntimeException.class, () -> 
                encryptionService.decrypt("not-valid-base64!@#$%"));
        }

        @Test
        @DisplayName("Should throw exception for corrupted ciphertext")
        void decryptCorruptedCiphertext() {
            // Valid Base64 but not valid ciphertext
            assertThrows(RuntimeException.class, () -> 
                encryptionService.decrypt("YWJjZGVmZ2hpamts"));
        }
    }

    @Nested
    @DisplayName("encrypt/decrypt round-trip")
    class RoundTripTests {

        @Test
        @DisplayName("Should successfully round-trip simple string")
        void roundTripSimpleString() {
            String original = "Hello, World!";
            String encrypted = encryptionService.encrypt(original);
            String decrypted = encryptionService.decrypt(encrypted);
            
            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("Should successfully round-trip OAuth token format")
        void roundTripOAuthToken() {
            String original = "ya29.a0AfH6SMBexampletoken123456789ABCDEF_ghijklmnop";
            String encrypted = encryptionService.encrypt(original);
            String decrypted = encryptionService.decrypt(encrypted);
            
            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("Should successfully round-trip long string")
        void roundTripLongString() {
            String original = "A".repeat(1000);
            String encrypted = encryptionService.encrypt(original);
            String decrypted = encryptionService.decrypt(encrypted);
            
            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("Should successfully round-trip special characters")
        void roundTripSpecialCharacters() {
            String original = "Special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?`~\n\t\r";
            String encrypted = encryptionService.encrypt(original);
            String decrypted = encryptionService.decrypt(encrypted);
            
            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("Should successfully round-trip Unicode characters")
        void roundTripUnicodeCharacters() {
            String original = "Unicode: 日本語 한국어 العربية 🎉🚀💻";
            String encrypted = encryptionService.encrypt(original);
            String decrypted = encryptionService.decrypt(encrypted);
            
            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("Should successfully round-trip JSON data")
        void roundTripJsonData() {
            String original = "{\"access_token\":\"token123\",\"refresh_token\":\"refresh456\",\"expires_in\":3600}";
            String encrypted = encryptionService.encrypt(original);
            String decrypted = encryptionService.decrypt(encrypted);
            
            assertEquals(original, decrypted);
        }
    }

    @Nested
    @DisplayName("Key handling")
    class KeyHandlingTests {

        @Test
        @DisplayName("Should work with short key (padded internally)")
        void shortKeyWorks() {
            EncryptionService shortKeyService = new EncryptionService("ShortKey");
            String original = "test data";
            
            String encrypted = shortKeyService.encrypt(original);
            String decrypted = shortKeyService.decrypt(encrypted);
            
            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("Should work with exact 32-byte key")
        void exactLengthKeyWorks() {
            EncryptionService exactKeyService = new EncryptionService("ExactlyThirtyTwoCharactersLong!!");
            String original = "test data";
            
            String encrypted = exactKeyService.encrypt(original);
            String decrypted = exactKeyService.decrypt(encrypted);
            
            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("Should work with long key (truncated internally)")
        void longKeyWorks() {
            EncryptionService longKeyService = new EncryptionService("ThisIsAVeryLongEncryptionKeyThatExceedsThirtyTwoBytes");
            String original = "test data";
            
            String encrypted = longKeyService.encrypt(original);
            String decrypted = longKeyService.decrypt(encrypted);
            
            assertEquals(original, decrypted);
        }

        @Test
        @DisplayName("Should use default key when empty string provided")
        void emptyKeyUsesDefault() {
            // Empty key should trigger default key usage
            EncryptionService defaultKeyService = new EncryptionService("");
            String original = "test data";
            
            String encrypted = defaultKeyService.encrypt(original);
            String decrypted = defaultKeyService.decrypt(encrypted);
            
            assertEquals(original, decrypted);
        }
    }
}
