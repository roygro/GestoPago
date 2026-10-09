package com.proyecto.servicios.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.proyecto.servicios.config.DosDecimalesSerializer;
import com.proyecto.servicios.model.cliente.SaldoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DosDecimalesSerializerTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        SimpleModule modulo = new SimpleModule();
        modulo.addSerializer(BigDecimal.class, new DosDecimalesSerializer());
        mapper = new ObjectMapper().registerModule(modulo);
    }

    /** Devuelve el JSON tal cual: las cantidades salen como texto, con comillas. */
    private String json(String valor) throws Exception {
        return mapper.writeValueAsString(new BigDecimal(valor));
    }

    @Test
    void enteros_llevanDosCeros() throws Exception {
        assertEquals("\"2000.00\"", json("2000"));
        assertEquals("\"0.00\"", json("0"));
    }

    @Test
    void unDecimal_seCompletaConCero() throws Exception {
        assertEquals("\"2000.50\"", json("2000.5"));
        assertEquals("\"0.10\"", json("0.1"));
    }

    @Test
    void yaConDosDecimales_quedaIgual() throws Exception {
        assertEquals("\"2000.00\"", json("2000.00"));
        assertEquals("\"1234.56\"", json("1234.56"));
    }

    @Test
    void masDeDosDecimales_seRedondeaHaciaArribaDesdeMedio() throws Exception {
        assertEquals("\"100.00\"", json("99.999"));
        assertEquals("\"99.99\"", json("99.994"));
        assertEquals("\"0.01\"", json("0.005"));
    }

    @Test
    void nuncaUsaNotacionCientifica() throws Exception {
        assertEquals("\"1000.00\"", json("1E+3"));
        assertEquals("\"123456789012.34\"", json("123456789012.34"));
    }

    @Test
    void enUnObjeto_elSaldoSaleConDosDecimales() throws Exception {
        SaldoResponse saldo = new SaldoResponse();
        saldo.setSaldoDisponible(new BigDecimal("500"));

        String json = mapper.writeValueAsString(saldo);

        assertTrue(json.contains("\"saldoDisponible\":\"500.00\""), json);
    }

    @Test
    void alRecibir_seSigueAceptandoNumeroOTexto() throws Exception {
        // El request puede mandar 2000, 2000.00 o "2000.00": el cambio es solo de salida.
        // (ObjectMapper de prueba SIN el serializador de salida, igual que la entrada real)
        ObjectMapper entrada = new ObjectMapper();
        assertEquals(0, new BigDecimal("2000").compareTo(entrada.readValue("2000", BigDecimal.class)));
        assertEquals(0, new BigDecimal("2000.00").compareTo(entrada.readValue("2000.00", BigDecimal.class)));
        assertEquals(0, new BigDecimal("2000.00").compareTo(entrada.readValue("\"2000.00\"", BigDecimal.class)));
    }
}
