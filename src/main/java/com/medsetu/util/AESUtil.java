package com.medsetu.util;

import org.springframework.stereotype.Component;

/**
 * Placeholder for AES encryption/decryption — to be implemented later
 * for encrypting sensitive fields like Aadhaar numbers.
 */
@Component
public class AESUtil {

    public String encrypt(String plainText) {
        // TODO: Implement AES-256 encryption
        return plainText;
    }

    public String decrypt(String cipherText) {
        // TODO: Implement AES-256 decryption
        return cipherText;
    }
}
