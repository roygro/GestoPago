package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.entity.sf.Domicilio;
import com.proyecto.servicios.entity.sf.Saldo;
import com.proyecto.servicios.exception.cliente.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.cliente.CorreoDuplicadoException;
import com.proyecto.servicios.exception.cliente.CurpDuplicadaException;
import com.proyecto.servicios.exception.cliente.RfcDuplicadoException;
import com.proyecto.servicios.model.cliente.*;
import com.proyecto.servicios.repositorys.sf.ClienteRepository;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.security.UsuarioAutenticado;
import com.proyecto.servicios.service.ClienteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.proyecto.servicios.repositorys.sf.ClienteSpecification;
import com.proyecto.servicios.exception.cliente.ValidacionNegocioException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.proyecto.servicios.exception.cliente.AccesoDenegadoException;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CuentaRepository cuentaRepository;

    @Autowired
    private UsuarioAutenticado usuarioAutenticado;

    @Value("${cuenta.saldo-inicial:0}")
    private BigDecimal saldoInicial;

    @Override
    @Transactional
    public ClienteResponse registrarCliente(ClienteRequest request) {
        if (clienteRepository.existsByCurp(request.getCurp())) {
            throw new CurpDuplicadaException(request.getCurp());
        }
        if (clienteRepository.existsByRfc(request.getRfc())) {
            throw new RfcDuplicadoException(request.getRfc());
        }

        if (clienteRepository.existsByCorreoElectronicoIgnoreCase(request.getCorreoElectronico())) {
            throw new CorreoDuplicadoException(request.getCorreoElectronico());
        }

        Cliente cliente = new Cliente();
        copiarDatosBasicos(cliente, request.getNombre(), request.getSegundoNombre(),
                request.getApellidoPaterno(), request.getApellidoMaterno());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setCurp(request.getCurp().toUpperCase());
        cliente.setRfc(request.getRfc().toUpperCase());
        cliente.setSexo(request.getSexo());
        cliente.setNacionalidad(request.getNacionalidad());
        cliente.setEstadoCivil(request.getEstadoCivil());
        cliente.setCorreoElectronico(request.getCorreoElectronico());
        cliente.setTelefonoMovil(request.getTelefonoMovil());
        cliente.setTelefonoAlternativo(request.getTelefonoAlternativo());
        cliente.setOcupacion(request.getOcupacion());
        cliente.setEmpresa(request.getEmpresa());
        cliente.setIngresoMensual(request.getIngresoMensual());
        cliente.setActivo(true);

        Domicilio domicilio = mapearDomicilio(new Domicilio(), request.getDomicilio());
        domicilio.setCliente(cliente);
        cliente.setDomicilio(domicilio);

        Cuenta cuenta = new Cuenta();
        cuenta.setNumeroCuenta(generarNumeroCuentaUnico());
        cuenta.setEstatus("ACTIVA");
        cuenta.setCliente(cliente);

        Saldo saldo = new Saldo();
        saldo.setSaldoDisponible(saldoInicial == null ? BigDecimal.ZERO : saldoInicial);
        saldo.setCuenta(cuenta);
        cuenta.setSaldo(saldo);

        cliente.getCuentas().add(cuenta);

        Cliente guardado = clienteRepository.save(cliente);
        log.info("Cliente registrado con id={} y cuenta={}", guardado.getId(), cuenta.getNumeroCuenta());
        return toClienteResponse(guardado);
    }

    @Override
    @Transactional
    public ClienteResponse actualizarCliente(ClienteActualizaRequest request) {
        usuarioAutenticado.validarPropietario(request.getId());
        Cliente cliente = buscarClienteOFallar(request.getId());

        if (!cliente.getCorreoElectronico().equalsIgnoreCase(request.getCorreoElectronico())
                && clienteRepository.existsByCorreoElectronicoIgnoreCase(request.getCorreoElectronico())) {
            throw new CorreoDuplicadoException(request.getCorreoElectronico());
        }

        copiarDatosBasicos(cliente, request.getNombre(), request.getSegundoNombre(),
                request.getApellidoPaterno(), request.getApellidoMaterno());
        cliente.setCorreoElectronico(request.getCorreoElectronico());
        cliente.setTelefonoMovil(request.getTelefonoMovil());
        cliente.setTelefonoAlternativo(request.getTelefonoAlternativo());
        cliente.setOcupacion(request.getOcupacion());
        cliente.setEmpresa(request.getEmpresa());
        cliente.setIngresoMensual(request.getIngresoMensual());

        mapearDomicilio(cliente.getDomicilio(), request.getDomicilio());

        return toClienteResponse(clienteRepository.save(cliente));
    }

    @Override
    @Transactional
    public void darDeBajaLogica(Long id) {
        usuarioAutenticado.validarPropietario(id);
        Cliente cliente = buscarClienteOFallar(id);
        cliente.setActivo(false);
        cliente.getCuentas().forEach(c -> c.setEstatus("INACTIVA"));
        clienteRepository.save(cliente);
        log.info("Cliente id={} dado de baja lógica", id);
    }

    // ---------- privados ----------

    private Cliente buscarClienteOFallar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("id " + id));
    }

    private void copiarDatosBasicos(Cliente cliente, String nombre, String segundoNombre,
                                    String apellidoPaterno, String apellidoMaterno) {
        cliente.setNombre(nombre);
        cliente.setSegundoNombre(segundoNombre);
        cliente.setApellidoPaterno(apellidoPaterno);
        cliente.setApellidoMaterno(apellidoMaterno);
    }

    private Domicilio mapearDomicilio(Domicilio domicilio, DomicilioDTO dto) {
        domicilio.setCalle(dto.getCalle());
        domicilio.setNumeroExterior(dto.getNumeroExterior());
        domicilio.setNumeroInterior(dto.getNumeroInterior());
        domicilio.setColonia(dto.getColonia());
        domicilio.setMunicipio(dto.getMunicipio());
        domicilio.setEstado(dto.getEstado());
        domicilio.setCodigoPostal(dto.getCodigoPostal());
        domicilio.setPais(dto.getPais());
        return domicilio;
    }

    private String generarNumeroCuentaUnico() {
        String numero;
        do {
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 16; i++) {
                sb.append(RANDOM.nextInt(10));
            }
            numero = sb.toString();
        } while (cuentaRepository.existsByNumeroCuenta(numero));
        return numero;
    }

    private DomicilioDTO toDomicilioDTO(Domicilio domicilio) {
        if (domicilio == null) return null;
        DomicilioDTO dto = new DomicilioDTO();
        dto.setCalle(domicilio.getCalle());
        dto.setNumeroExterior(domicilio.getNumeroExterior());
        dto.setNumeroInterior(domicilio.getNumeroInterior());
        dto.setColonia(domicilio.getColonia());
        dto.setMunicipio(domicilio.getMunicipio());
        dto.setEstado(domicilio.getEstado());
        dto.setCodigoPostal(domicilio.getCodigoPostal());
        dto.setPais(domicilio.getPais());
        return dto;
    }

    private SaldoResponse toSaldoResponse(Saldo saldo) {
        if (saldo == null) return null;
        SaldoResponse dto = new SaldoResponse();
        dto.setSaldoDisponible(saldo.getSaldoDisponible());
        dto.setFechaActualizacion(saldo.getFechaActualizacion());
        return dto;
    }

    private CuentaResponse toCuentaResponse(Cuenta cuenta) {
        CuentaResponse dto = new CuentaResponse();
        dto.setId(cuenta.getId());
        dto.setNumeroCuenta(cuenta.getNumeroCuenta());
        dto.setFechaApertura(cuenta.getFechaApertura());
        dto.setEstatus(cuenta.getEstatus());
        dto.setSaldo(toSaldoResponse(cuenta.getSaldo()));
        return dto;
    }

    private ClienteResponse toClienteResponse(Cliente cliente) {
        ClienteResponse dto = new ClienteResponse();
        dto.setId(cliente.getId());
        dto.setNombre(cliente.getNombre());
        dto.setSegundoNombre(cliente.getSegundoNombre());
        dto.setApellidoPaterno(cliente.getApellidoPaterno());
        dto.setApellidoMaterno(cliente.getApellidoMaterno());
        dto.setFechaNacimiento(cliente.getFechaNacimiento());
        dto.setCurp(cliente.getCurp());
        dto.setRfc(cliente.getRfc());
        dto.setSexo(cliente.getSexo());
        dto.setNacionalidad(cliente.getNacionalidad());
        dto.setEstadoCivil(cliente.getEstadoCivil());
        dto.setCorreoElectronico(cliente.getCorreoElectronico());
        dto.setTelefonoMovil(cliente.getTelefonoMovil());
        dto.setTelefonoAlternativo(cliente.getTelefonoAlternativo());
        dto.setOcupacion(cliente.getOcupacion());
        dto.setEmpresa(cliente.getEmpresa());
        dto.setIngresoMensual(cliente.getIngresoMensual());
        dto.setActivo(cliente.getActivo());
        dto.setFechaRegistro(cliente.getFechaRegistro());
        dto.setFechaActualizacion(cliente.getFechaActualizacion());
        dto.setDomicilio(toDomicilioDTO(cliente.getDomicilio()));
        dto.setCuentas(cliente.getCuentas().stream().map(this::toCuentaResponse).collect(Collectors.toList()));
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponse> buscarClientes(ClienteBusquedaRequest filtros) {
        // Un cliente solo puede buscar dentro de su propia información
        Cliente propio = buscarClienteOFallar(usuarioAutenticado.obtenerClienteId());
        validarFiltrosPropios(filtros, propio);
        filtros.setId(propio.getId());

        if (filtros.getFechaRegistroDesde() != null && filtros.getFechaRegistroHasta() != null
                && filtros.getFechaRegistroDesde().isAfter(filtros.getFechaRegistroHasta())) {
            throw new ValidacionNegocioException("La fecha inicial no puede ser posterior a la fecha final");
        }

        List<Cliente> clientes = clienteRepository.findAll(ClienteSpecification.conFiltros(filtros));
        if (clientes.isEmpty()) {
            throw new ClienteNoEncontradoException("no hay coincidencias con los criterios indicados");
        }
        return clientes.stream().map(this::toClienteResponse).collect(Collectors.toList());
    }

    /**
     * Si algún criterio identificador (id, CURP, RFC, correo o cuenta) no coincide con los datos
     * del cliente autenticado, responde 403 sin revelar si ese dato existe en el sistema.
     */
    private void validarFiltrosPropios(ClienteBusquedaRequest f, Cliente propio) {
        boolean ajeno =
                (f.getId() != null && !propio.getId().equals(f.getId()))
                        || (StringUtils.hasText(f.getCurp())
                        && !f.getCurp().trim().equalsIgnoreCase(propio.getCurp()))
                        || (StringUtils.hasText(f.getRfc())
                        && !f.getRfc().trim().equalsIgnoreCase(propio.getRfc()))
                        || (StringUtils.hasText(f.getCorreoElectronico())
                        && !f.getCorreoElectronico().trim().equalsIgnoreCase(propio.getCorreoElectronico()))
                        || (StringUtils.hasText(f.getNumeroCuenta())
                        && propio.getCuentas().stream()
                        .noneMatch(c -> c.getNumeroCuenta().equals(f.getNumeroCuenta().trim())));

        if (ajeno) {
            throw new AccesoDenegadoException("No tiene permiso para consultar información de otro cliente");
        }
    }

}