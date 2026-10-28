package com.proyecto.servicios.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Helper de cifrado y descifrado AES-256 en modo CBC con padding PKCS5.
 * Basado en la especificación de seguridad de PuntoRed / GestoPago.
 */
public final class AesEncryptionHelper {

    private static final String ALGORITMO_TRANSFORMACION = "AES/CBC/PKCS5Padding";
    private static final String ALGORITMO_LLAVE = "AES";

    private AesEncryptionHelper() {
    }

    /**
     * Cifra un texto en claro (típicamente una cadena JSON) usando AES/CBC/PKCS5Padding
     * con la llave y el vector de inicialización provistos en Base64.
     *
     * @param plainText       Texto en formato JSON a cifrar
     * @param secretKeyBase64 Llave de cifrado en formato Base64 (256 bits)
     * @param ivBase64        Vector de inicialización en formato Base64 (128 bits)
     * @return Cadena cifrada codificada en Base64
     */
    public static String encrypt(String plainText, String secretKeyBase64, String ivBase64) {
        if (plainText == null) {
            throw new IllegalArgumentException("El texto a cifrar no puede ser nulo");
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64);
            byte[] ivBytes = Base64.getDecoder().decode(ivBase64);

            SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, ALGORITMO_LLAVE);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(ALGORITMO_TRANSFORMACION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar el payload con AES: " + e.getMessage(), e);
        }
    }

    /**
     * Descifra una cadena en Base64 previamente cifrada con AES/CBC/PKCS5Padding.
     *
     * @param encryptedBase64 Cadena cifrada en Base64
     * @param secretKeyBase64 Llave de cifrado en Base64
     * @param ivBase64        Vector de inicialización en Base64
     * @return Texto en claro decodificado en UTF-8
     */
    public static String decrypt(String encryptedBase64, String secretKeyBase64, String ivBase64) {
        if (encryptedBase64 == null) {
            throw new IllegalArgumentException("El texto cifrado no puede ser nulo");
        }
        try {
            byte[] keyBytes = Base64.getDecoder().decode(secretKeyBase64);
            byte[] ivBytes = Base64.getDecoder().decode(ivBase64);

            SecretKey secretKey = new SecretKeySpec(keyBytes, 0, keyBytes.length, ALGORITMO_LLAVE);
            IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(ALGORITMO_TRANSFORMACION);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedBase64));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error al descifrar el payload con AES: " + e.getMessage(), e);
        }
    }
}
