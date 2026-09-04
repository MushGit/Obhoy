package com.obhoy.app.util

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128
    private const val IV_LENGTH_BYTES = 12

    /**
     * Generates a cryptographically secure 256-bit AES key.
     */
    fun generateRandomKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance("AES")
        keyGenerator.init(256)
        return keyGenerator.generateKey()
    }

    /**
     * Generates a cryptographically secure random byte array.
     */
    fun generateSecureRandomBytes(length: Int): ByteArray {
        val bytes = ByteArray(length)
        SecureRandom().nextBytes(bytes)
        return bytes
    }

    /**
     * Encrypts plain byte array using AES-256 GCM.
     * Returns concatenated [IV (12 bytes) + Ciphertext].
     */
    fun encryptAesGcm(data: ByteArray, secretKey: SecretKey): ByteArray {
        val iv = generateSecureRandomBytes(IV_LENGTH_BYTES)
        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec)
        val ciphertext = cipher.doFinal(data)

        // Concatenate IV + Ciphertext
        return iv + ciphertext
    }

    /**
     * Decrypts AES-256 GCM payload containing [IV (12 bytes) + Ciphertext].
     */
    fun decryptAesGcm(encryptedData: ByteArray, secretKey: SecretKey): ByteArray {
        require(encryptedData.size > IV_LENGTH_BYTES) { "Invalid encrypted data length" }

        val iv = encryptedData.copyOfRange(0, IV_LENGTH_BYTES)
        val ciphertext = encryptedData.copyOfRange(IV_LENGTH_BYTES, encryptedData.size)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec)
        return cipher.doFinal(ciphertext)
    }

    /**
     * Securely zeroes out sensitive byte arrays in memory to prevent cold-boot/dump inspection.
     */
    fun wipeByteArray(array: ByteArray) {
        array.fill(0)
    }

    /**
     * Encodes a byte array to Base64 String.
     */
    fun toBase64(bytes: ByteArray): String {
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Decodes a Base64 String to byte array.
     */
    fun fromBase64(base64Str: String): ByteArray {
        return Base64.decode(base64Str, Base64.NO_WRAP)
    }
}

