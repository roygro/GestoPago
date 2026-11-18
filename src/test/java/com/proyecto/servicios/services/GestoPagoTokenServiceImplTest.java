package com.proyecto.servicios.services;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.Impl.GestoPagoTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GestoPagoTokenServiceImplTest {

    private GestoPagoAuthClient authClient;
    private GestoPagoTokenRepository tokenRepository;
    private GestoPagoTokenMapper tokenMapper;
    private GestoPagoTokenServiceImpl service;

    @BeforeEach
    void setUp() {
        authClient = mock(GestoPagoAuthClient.class);
        tokenRepository = mock(GestoPagoTokenRepository.class);
        tokenMapper = mock(GestoPagoTokenMapper.class);
        service = new GestoPagoTokenServiceImpl(authClient, tokenRepository, tokenMapper);

        ReflectionTestUtils.setField(service, "idDistribuidor", 83);
        ReflectionTestUtils.setField(service, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(service, "password", "12345678");
    }

    @Test
    @DisplayName("renovarToken crea un nuevo registro si no existía token previo")
    void renovarToken_nuevoToken() {
        GestoPagoAuthResponse authResponse = new GestoPagoAuthResponse();
        authResponse.setToken("nuevo_token_123");

        when(authClient.authenticate(83, "GPS83-TPV-17", "12345678")).thenReturn(authResponse);
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17"))
                .thenReturn(Optional.empty());

        GestoPagoToken nuevoToken = new GestoPagoToken();
        when(tokenMapper.toEntity(authResponse)).thenReturn(nuevoToken);

        service.renovarToken();

        verify(tokenRepository).save(nuevoToken);
        assertEquals(83, nuevoToken.getIdDistribuidor());
        assertEquals("GPS83-TPV-17", nuevoToken.getCodigoDispositivo());
        assertTrue(nuevoToken.getActivo());
    }

    @Test
    @DisplayName("renovarToken actualiza registro existente si ya existía")
    void renovarToken_actualizaExistente() {
        GestoPagoAuthResponse authResponse = new GestoPagoAuthResponse();
        authResponse.setToken("token_actualizado");

        GestoPagoToken existing = new GestoPagoToken();
        existing.setId(1);

        when(authClient.authenticate(83, "GPS83-TPV-17", "12345678")).thenReturn(authResponse);
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17"))
                .thenReturn(Optional.of(existing));

        service.renovarToken();

        verify(tokenMapper).updateEntity(authResponse, existing);
        verify(tokenRepository).save(existing);
    }

    @Test
    @DisplayName("renovarToken no guarda si respuesta es null o token es null")
    void renovarToken_respuestaVacia_noGuarda() {
        when(authClient.authenticate(83, "GPS83-TPV-17", "12345678")).thenReturn(null);

        service.renovarToken();

        verify(tokenRepository, never()).save(any());

        GestoPagoAuthResponse respSinToken = new GestoPagoAuthResponse();
        when(authClient.authenticate(83, "GPS83-TPV-17", "12345678")).thenReturn(respSinToken);

        service.renovarToken();

        verify(tokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("renovarToken captura excepciones del cliente y no propaga error")
    void renovarToken_conExcepcion_noFalla() {
        when(authClient.authenticate(anyInt(), anyString(), anyString()))
                .thenThrow(new RuntimeException("Error de red"));

        assertDoesNotThrow(() -> service.renovarToken());
    }

    @Test
    @DisplayName("obtenerTokenActivo consulta al repositorio")
    void obtenerTokenActivo_exito() {
        GestoPagoToken token = new GestoPagoToken();
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17"))
                .thenReturn(Optional.of(token));

        Optional<GestoPagoToken> result = service.obtenerTokenActivo(83, "GPS83-TPV-17");

        assertTrue(result.isPresent());
        assertSame(token, result.get());
    }
}
