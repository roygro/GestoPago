package com.proyecto.servicios.services;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.entity.sf.Domicilio;
import com.proyecto.servicios.entity.sf.Saldo;
import com.proyecto.servicios.exception.cliente.*;
import com.proyecto.servicios.model.cliente.*;
import com.proyecto.servicios.repositorys.sf.ClienteRepository;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.security.UsuarioAutenticado;
import com.proyecto.servicios.service.Impl.ClienteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ClienteServiceImplTest {

    private ClienteRepository clienteRepository;
    private CuentaRepository cuentaRepository;
    private UsuarioAutenticado usuarioAutenticado;
    private ClienteServiceImpl service;

    @BeforeEach
    void setUp() {
        clienteRepository = mock(ClienteRepository.class);
        cuentaRepository = mock(CuentaRepository.class);
        usuarioAutenticado = mock(UsuarioAutenticado.class);

        service = new ClienteServiceImpl();
        ReflectionTestUtils.setField(service, "clienteRepository", clienteRepository);
        ReflectionTestUtils.setField(service, "cuentaRepository", cuentaRepository);
        ReflectionTestUtils.setField(service, "usuarioAutenticado", usuarioAutenticado);
        ReflectionTestUtils.setField(service, "saldoInicial", new BigDecimal("500.00"));
    }

    private ClienteRequest crearRequestRegistro() {
        ClienteRequest req = new ClienteRequest();
        req.setNombre("Juan");
        req.setSegundoNombre("Carlos");
        req.setApellidoPaterno("Perez");
        req.setApellidoMaterno("Lopez");
        req.setFechaNacimiento(LocalDate.of(1990, 1, 15));
        req.setCurp("PELJ900115HDFRPA01");
        req.setRfc("PELJ900115XX1");
        req.setSexo("H");
        req.setNacionalidad("MEXICANA");
        req.setEstadoCivil("SOLTERO");
        req.setCorreoElectronico("juan@correo.com");
        req.setTelefonoMovil("5512345678");
        req.setTelefonoAlternativo("5587654321");
        req.setOcupacion("Ingeniero");
        req.setEmpresa("TechCorp");
        req.setIngresoMensual(new BigDecimal("35000.00"));

        DomicilioDTO dom = new DomicilioDTO();
        dom.setCalle("Av Reforma");
        dom.setNumeroExterior("123");
        dom.setNumeroInterior("4B");
        dom.setColonia("Juarez");
        dom.setMunicipio("Cuauhtemoc");
        dom.setEstado("CDMX");
        dom.setCodigoPostal("06600");
        dom.setPais("MEXICO");
        req.setDomicilio(dom);
        return req;
    }

    @Test
    @DisplayName("registrarCliente guarda y retorna DTO cuando todos los datos son válidos")
    void registrarCliente_exito() {
        ClienteRequest req = crearRequestRegistro();

        when(clienteRepository.existsByCurp(req.getCurp())).thenReturn(false);
        when(clienteRepository.existsByRfc(req.getRfc())).thenReturn(false);
        when(clienteRepository.existsByCorreoElectronicoIgnoreCase(req.getCorreoElectronico())).thenReturn(false);
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);

        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente c = invocation.getArgument(0);
            c.setId(100L);
            c.setFechaRegistro(LocalDateTime.now());
            c.setFechaActualizacion(LocalDateTime.now());
            return c;
        });

        ClienteResponse response = service.registrarCliente(req);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("JUAN", response.getNombre().toUpperCase());
        assertEquals(req.getCurp().toUpperCase(), response.getCurp());
        assertEquals(req.getRfc().toUpperCase(), response.getRfc());
        assertTrue(response.getActivo());
        assertNotNull(response.getDomicilio());
        assertEquals("Av Reforma", response.getDomicilio().getCalle());
        assertFalse(response.getCuentas().isEmpty());
        assertEquals(new BigDecimal("500.00"), response.getCuentas().get(0).getSaldo().getSaldoDisponible());
    }

    @Test
    @DisplayName("registrarCliente lanza CurpDuplicadaException si el CURP ya existe")
    void registrarCliente_curpDuplicada() {
        ClienteRequest req = crearRequestRegistro();
        when(clienteRepository.existsByCurp(req.getCurp())).thenReturn(true);

        assertThrows(CurpDuplicadaException.class, () -> service.registrarCliente(req));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("registrarCliente lanza RfcDuplicadoException si el RFC ya existe")
    void registrarCliente_rfcDuplicado() {
        ClienteRequest req = crearRequestRegistro();
        when(clienteRepository.existsByCurp(req.getCurp())).thenReturn(false);
        when(clienteRepository.existsByRfc(req.getRfc())).thenReturn(true);

        assertThrows(RfcDuplicadoException.class, () -> service.registrarCliente(req));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("registrarCliente lanza CorreoDuplicadoException si el correo ya existe")
    void registrarCliente_correoDuplicado() {
        ClienteRequest req = crearRequestRegistro();
        when(clienteRepository.existsByCurp(req.getCurp())).thenReturn(false);
        when(clienteRepository.existsByRfc(req.getRfc())).thenReturn(false);
        when(clienteRepository.existsByCorreoElectronicoIgnoreCase(req.getCorreoElectronico())).thenReturn(true);

        assertThrows(CorreoDuplicadoException.class, () -> service.registrarCliente(req));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarCliente actualiza los datos del cliente autenticado")
    void actualizarCliente_exito() {
        ClienteActualizaRequest req = new ClienteActualizaRequest();
        req.setId(10L);
        req.setNombre("Juan");
        req.setSegundoNombre("Manuel");
        req.setApellidoPaterno("Perez");
        req.setApellidoMaterno("Lopez");
        req.setCorreoElectronico("nuevo_juan@correo.com");
        req.setTelefonoMovil("5599887766");
        req.setOcupacion("Senior Architect");
        req.setEmpresa("BigCorp");
        req.setIngresoMensual(new BigDecimal("50000.00"));

        DomicilioDTO dom = new DomicilioDTO();
        dom.setCalle("Calle Nueva");
        req.setDomicilio(dom);

        Cliente existente = new Cliente();
        existente.setId(10L);
        existente.setCorreoElectronico("viejo_juan@correo.com");
        existente.setDomicilio(new Domicilio());

        when(clienteRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(clienteRepository.existsByCorreoElectronicoIgnoreCase("nuevo_juan@correo.com")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponse response = service.actualizarCliente(req);

        assertNotNull(response);
        verify(usuarioAutenticado).validarPropietario(10L);
        assertEquals("nuevo_juan@correo.com", response.getCorreoElectronico());
        assertEquals("Senior Architect", response.getOcupacion());
    }

    @Test
    @DisplayName("actualizarCliente lanza CorreoDuplicadoException si el nuevo correo pertenece a otro usuario")
    void actualizarCliente_correoDuplicado() {
        ClienteActualizaRequest req = new ClienteActualizaRequest();
        req.setId(10L);
        req.setCorreoElectronico("repetido@correo.com");

        Cliente existente = new Cliente();
        existente.setId(10L);
        existente.setCorreoElectronico("original@correo.com");

        when(clienteRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(clienteRepository.existsByCorreoElectronicoIgnoreCase("repetido@correo.com")).thenReturn(true);

        assertThrows(CorreoDuplicadoException.class, () -> service.actualizarCliente(req));
    }

    @Test
    @DisplayName("darDeBajaLogica marca cliente y sus cuentas como inactivas")
    void darDeBajaLogica_exito() {
        Cliente cliente = new Cliente();
        cliente.setId(10L);
        cliente.setActivo(true);

        Cuenta c1 = new Cuenta();
        c1.setEstatus("ACTIVA");
        cliente.getCuentas().add(c1);

        when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente));

        service.darDeBajaLogica(10L);

        assertFalse(cliente.getActivo());
        assertEquals("INACTIVA", c1.getEstatus());
        verify(usuarioAutenticado).validarPropietario(10L);
        verify(clienteRepository).save(cliente);
    }

    @Test
    @DisplayName("darDeBajaLogica lanza ClienteNoEncontradoException si no existe")
    void darDeBajaLogica_noExiste() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ClienteNoEncontradoException.class, () -> service.darDeBajaLogica(99L));
    }

    @Test
    @DisplayName("buscarClientes retorna lista de clientes cuando los filtros corresponden al propio usuario")
    void buscarClientes_exito() {
        Cliente propio = new Cliente();
        propio.setId(10L);
        propio.setCurp("PELJ900115HDFRPA01");
        propio.setRfc("PELJ900115XX1");
        propio.setCorreoElectronico("juan@correo.com");

        Cuenta c = new Cuenta();
        c.setNumeroCuenta("1234567890123456");
        propio.getCuentas().add(c);

        when(usuarioAutenticado.obtenerClienteId()).thenReturn(10L);
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(propio));

        ClienteBusquedaRequest filtros = new ClienteBusquedaRequest();
        filtros.setId(10L);
        filtros.setCurp("PELJ900115HDFRPA01");

        when(clienteRepository.findAll(any(Specification.class))).thenReturn(Collections.singletonList(propio));

        List<ClienteResponse> resultados = service.buscarClientes(filtros);

        assertFalse(resultados.isEmpty());
        assertEquals(1, resultados.size());
    }

    @Test
    @DisplayName("buscarClientes lanza AccesoDenegadoException si los filtros intentan buscar otro cliente")
    void buscarClientes_intentoBuscarOtroCliente() {
        Cliente propio = new Cliente();
        propio.setId(10L);
        propio.setCurp("PELJ900115HDFRPA01");

        when(usuarioAutenticado.obtenerClienteId()).thenReturn(10L);
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(propio));

        ClienteBusquedaRequest filtros = new ClienteBusquedaRequest();
        filtros.setId(99L); // ID ajeno

        assertThrows(AccesoDenegadoException.class, () -> service.buscarClientes(filtros));
    }

    @Test
    @DisplayName("buscarClientes lanza ValidacionNegocioException si fechaRegistroDesde es posterior a fechaRegistroHasta")
    void buscarClientes_fechasInvalidas() {
        Cliente propio = new Cliente();
        propio.setId(10L);

        when(usuarioAutenticado.obtenerClienteId()).thenReturn(10L);
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(propio));

        ClienteBusquedaRequest filtros = new ClienteBusquedaRequest();
        filtros.setFechaRegistroDesde(LocalDateTime.now().plusDays(5));
        filtros.setFechaRegistroHasta(LocalDateTime.now());

        assertThrows(ValidacionNegocioException.class, () -> service.buscarClientes(filtros));
    }

    @Test
    @DisplayName("buscarClientes lanza ClienteNoEncontradoException cuando no hay resultados en la BD")
    void buscarClientes_sinResultados() {
        Cliente propio = new Cliente();
        propio.setId(10L);

        when(usuarioAutenticado.obtenerClienteId()).thenReturn(10L);
        when(clienteRepository.findById(10L)).thenReturn(Optional.of(propio));

        ClienteBusquedaRequest filtros = new ClienteBusquedaRequest();
        when(clienteRepository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());

        assertThrows(ClienteNoEncontradoException.class, () -> service.buscarClientes(filtros));
    }
}
