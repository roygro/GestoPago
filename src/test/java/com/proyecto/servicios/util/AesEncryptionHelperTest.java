package com.proyecto.servicios.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AesEncryptionHelperTest {

    private static final String SECRET_KEY = "HPo7OLqB4Fkk4E2yGOtwqw8H5fHR9kNx67OR4g4UdlA=";
    private static final String IV = "p5ldmBPdd/9pjC0bDC/nSg==";

    @Test
    @DisplayName("Cifrado y descifrado simétrico AES-256 recuperan el texto original")
    void cifrarYDescifrarExitoso() {
        String jsonOriginal = "{\"idProducto\":582,\"idServicio\":133,\"telefono\":\"5555555551\",\"montoPago\":10.0}";

        String cifrado = AesEncryptionHelper.encrypt(jsonOriginal, SECRET_KEY, IV);
        assertNotNull(cifrado);
        assertFalse(cifrado.isBlank());
        assertNotEquals(jsonOriginal, cifrado);

        String descifrado = AesEncryptionHelper.decrypt(cifrado, SECRET_KEY, IV);
        assertEquals(jsonOriginal, descifrado);
    }

    @Test
    @DisplayName("Cifrar con texto nulo lanza IllegalArgumentException")
    void cifrarTextoNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                AesEncryptionHelper.encrypt(null, SECRET_KEY, IV));
    }

    @Test
    @DisplayName("Descifrar con texto nulo lanza IllegalArgumentException")
    void descifrarTextoNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () ->
                AesEncryptionHelper.decrypt(null, SECRET_KEY, IV));
    }

    @Test
    @DisplayName("Cifrar con clave o IV inválidos lanza RuntimeException")
    void cifrarConClaveInvalidaLanzaExcepcion() {
        assertThrows(RuntimeException.class, () ->
                AesEncryptionHelper.encrypt("{}", "claveInvalida", IV));
    }
}
