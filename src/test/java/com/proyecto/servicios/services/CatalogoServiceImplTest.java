package com.proyecto.servicios.services;

import com.proyecto.servicios.entity.sf.EstadoCivil;
import com.proyecto.servicios.entity.sf.Nacionalidad;
import com.proyecto.servicios.model.cliente.EstadoCivilResponse;
import com.proyecto.servicios.model.cliente.NacionalidadResponse;
import com.proyecto.servicios.repositorys.sf.EstadoCivilRepository;
import com.proyecto.servicios.repositorys.sf.NacionalidadRepository;
import com.proyecto.servicios.service.Impl.CatalogoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CatalogoServiceImplTest {

    private EstadoCivilRepository estadoCivilRepository;
    private NacionalidadRepository nacionalidadRepository;
    private CatalogoServiceImpl service;

    @BeforeEach
    void setUp() {
        estadoCivilRepository = mock(EstadoCivilRepository.class);
        nacionalidadRepository = mock(NacionalidadRepository.class);
        service = new CatalogoServiceImpl();
        ReflectionTestUtils.setField(service, "estadoCivilRepository", estadoCivilRepository);
        ReflectionTestUtils.setField(service, "nacionalidadRepository", nacionalidadRepository);
    }

    private EstadoCivil estado(String clave, String descripcion) {
        EstadoCivil e = new EstadoCivil();
        e.setClave(clave);
        e.setDescripcion(descripcion);
        e.setActivo(true);
        return e;
    }

    private Nacionalidad nacionalidad(String gentilicio, String pais, String codigoPais) {
        Nacionalidad n = new Nacionalidad();
        n.setNacionalidad(gentilicio);
        n.setPais(pais);
        n.setCodigoPais(codigoPais);
        n.setActivo(true);
        return n;
    }

    @Test
    void consultarEstadosCiviles_mapeaClaveYDescripcionEnElMismoOrden() {
        when(estadoCivilRepository.findByActivoTrueOrderByIdAsc())
                .thenReturn(List.of(estado("SOLTERO", "Soltero(a)"), estado("CASADO", "Casado(a)")));

        List<EstadoCivilResponse> resultado = service.consultarEstadosCiviles();

        assertEquals(2, resultado.size());
        assertEquals("SOLTERO", resultado.get(0).getClave());
        assertEquals("Soltero(a)", resultado.get(0).getDescripcion());
        assertEquals("CASADO", resultado.get(1).getClave());
    }

    @Test
    void consultarEstadosCiviles_sinRegistros_devuelveListaVacia() {
        when(estadoCivilRepository.findByActivoTrueOrderByIdAsc()).thenReturn(List.of());

        assertTrue(service.consultarEstadosCiviles().isEmpty());
    }

    @Test
    void consultarNacionalidades_devuelveElGentilicioYElPais_sinExponerElCodigoIso() {
        when(nacionalidadRepository.findByActivoTrueOrderByPaisAsc())
                .thenReturn(List.of(nacionalidad("Mexicano", "México", "MX"),
                                    nacionalidad("Estadounidense", "Estados Unidos", "US")));

        List<NacionalidadResponse> resultado = service.consultarNacionalidades();

        assertEquals(2, resultado.size());
        assertEquals("Mexicano", resultado.get(0).getNacionalidad());
        assertEquals("México", resultado.get(0).getPais());
        assertEquals("Estadounidense", resultado.get(1).getNacionalidad());
    }

    @Test
    void consultarNacionalidades_sinRegistros_devuelveListaVacia() {
        when(nacionalidadRepository.findByActivoTrueOrderByPaisAsc()).thenReturn(List.of());

        assertTrue(service.consultarNacionalidades().isEmpty());
    }
}
