package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Login;
import com.proyecto.servicios.exception.cliente.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.cliente.ValidacionNegocioException;
import com.proyecto.servicios.model.cliente.LoginRequest;
import com.proyecto.servicios.model.cliente.LoginResponse;
import com.proyecto.servicios.repositorys.sf.ClienteRepository;
import com.proyecto.servicios.repositorys.sf.LoginRepository;
import com.proyecto.servicios.security.EstadoSesion;
import com.proyecto.servicios.security.JwtUtil;
import com.proyecto.servicios.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class LoginServiceImpl implements LoginService {

    @Autowired
    private LoginRepository loginRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private Clock clock;

    /** Minutos de inactividad permitidos antes de apagar la bandera de sesión (por defecto 5). */
    @Value("${sesion.inactividad-minutos:5}")
    private int minutosInactividad;

    @Override
    @Transactional
    public LoginResponse crearCredenciales(Long clienteId, String usuario, String passwordPlano) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ClienteNoEncontradoException("id " + clienteId));

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ValidacionNegocioException("No se pueden crear credenciales para un cliente inactivo");
        }

        // El usuario debe ser único entre clientes: autenticar() toma el último login de ese usuario.
        if (loginRepository.existsByUsuarioAndClienteIdNot(usuario, clienteId)) {
            throw new ValidacionNegocioException("El usuario ya está en uso por otro cliente");
        }

        return crearNuevaSesion(cliente, usuario, passwordEncoder.encode(passwordPlano), null, null, null, null, null);
    }

    @Override
    @Transactional
    public LoginResponse autenticar(LoginRequest request) {
        Login ultimoLogin = loginRepository.findTopByUsuarioOrderByFechaInicioSesionDesc(request.getUsuario())
                .orElseThrow(() -> new ValidacionNegocioException("El usuario no existe"));

        if (!passwordEncoder.matches(request.getPassword(), ultimoLogin.getPasswordHash())) {
            throw new ValidacionNegocioException("La contraseña es incorrecta");
        }

        Cliente cliente = ultimoLogin.getCliente();
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ValidacionNegocioException("El cliente está inactivo, no puede iniciar sesión");
        }

        // Cierra cualquier sesión anterior que siga activa para ese cliente
        List<Login> sesionesActivas = loginRepository.findByClienteIdAndSesionActivaTrue(cliente.getId());
        sesionesActivas.forEach(s -> s.setSesionActiva(false));
        loginRepository.saveAll(sesionesActivas);

        return crearNuevaSesion(cliente, request.getUsuario(), ultimoLogin.getPasswordHash(),
                request.getDistanciaInterocular(), request.getAnchoRostro(),
                request.getConfianzaDeteccion(), request.getNumPuntosReferencia(), request.getPlantillaFacial());
    }

    @Override
    @Transactional
    public void cerrarSesion(String jwtToken) {
        Login login = loginRepository.findByJwtToken(jwtToken)
                .orElseThrow(() -> new ValidacionNegocioException("Sesión no encontrada"));
        login.setSesionActiva(false);
        loginRepository.save(login);
    }

    /**
     * Regla de sesión:
     *  - La bandera sesion_activa nace en true al hacer login.
     *  - Pasa a false al hacer logout, al hacer un nuevo login, al dar de baja al cliente
     *    o cuando pasan los minutos de inactividad sin ninguna petición.
     *  - Cada petición válida renueva ultima_actividad (ventana deslizante).
     *
     * Si la bandera ya está en false se distingue el motivo: si además ya pasó la ventana de
     * inactividad se responde INACTIVIDAD; si no, CERRADA (logout o nuevo login).
     */
    @Override
    @Transactional
    public EstadoSesion validarSesion(String jwtToken) {
        Optional<Login> encontrado = loginRepository.findByJwtToken(jwtToken);
        if (encontrado.isEmpty()) {
            return EstadoSesion.NO_ENCONTRADA;
        }
        Login login = encontrado.get();

        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDateTime limite = login.getUltimaActividad().plusMinutes(login.getMinutosExpiracionInactividad());
        boolean vencidaPorInactividad = ahora.isAfter(limite);

        if (!Boolean.TRUE.equals(login.getSesionActiva())) {
            return vencidaPorInactividad ? EstadoSesion.INACTIVIDAD : EstadoSesion.CERRADA;
        }

        // Si el cliente fue dado de baja, su sesión deja de valer aunque el token no haya expirado.
        if (login.getCliente() != null && !Boolean.TRUE.equals(login.getCliente().getActivo())) {
            login.setSesionActiva(false);
            loginRepository.save(login);
            return EstadoSesion.CLIENTE_INACTIVO;
        }

        if (vencidaPorInactividad) {
            login.setSesionActiva(false);
            loginRepository.save(login);
            log.info("Sesión cerrada por inactividad. loginId={}", login.getId());
            return EstadoSesion.INACTIVIDAD;
        }

        login.setUltimaActividad(ahora);
        loginRepository.save(login);
        return EstadoSesion.VALIDA;
    }

    @Override
    @Transactional
    public int cerrarSesionesVencidas() {
        return loginRepository.cerrarSesionesVencidas(LocalDateTime.now(clock));
    }

    private LoginResponse crearNuevaSesion(Cliente cliente, String usuario, String passwordHash,
                                           Double distanciaInterocular, Double anchoRostro,
                                           java.math.BigDecimal confianzaDeteccion,
                                           Integer numPuntosReferencia, String plantillaFacial) {
        String token = jwtUtil.generarToken(cliente.getId(), usuario);
        LocalDateTime expiracion = jwtUtil.obtenerFechaExpiracion(token);

        Login login = new Login();
        login.setCliente(cliente);
        login.setUsuario(usuario);
        login.setPasswordHash(passwordHash);
        login.setJwtToken(token);
        login.setJwtFechaExpiracion(expiracion);
        login.setSesionActiva(true);
        login.setMinutosExpiracionInactividad(minutosInactividad);
        login.setDistanciaInterocular(distanciaInterocular);
        login.setAnchoRostro(anchoRostro);
        login.setConfianzaDeteccion(confianzaDeteccion);
        login.setNumPuntosReferencia(numPuntosReferencia);
        login.setPlantillaFacial(plantillaFacial);

        loginRepository.save(login);
        log.info("Sesión creada para cliente id={} usuario={}", cliente.getId(), usuario);

        LoginResponse response = new LoginResponse();
        response.setJwtToken(token);
        response.setJwtFechaExpiracion(expiracion);
        response.setSesionActiva(true);
        response.setMinutosExpiracionInactividad(login.getMinutosExpiracionInactividad());
        return response;
    }
}
