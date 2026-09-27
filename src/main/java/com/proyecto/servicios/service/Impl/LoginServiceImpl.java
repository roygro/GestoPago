package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Login;
import com.proyecto.servicios.exception.cliente.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.cliente.ValidacionNegocioException;
import com.proyecto.servicios.model.cliente.LoginRequest;
import com.proyecto.servicios.model.cliente.LoginResponse;
import com.proyecto.servicios.repositorys.sf.ClienteRepository;
import com.proyecto.servicios.repositorys.sf.LoginRepository;
import com.proyecto.servicios.security.JwtUtil;
import com.proyecto.servicios.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

    @Override
    @Transactional
    public LoginResponse crearCredenciales(Long clienteId, String usuario, String passwordPlano) {
        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ClienteNoEncontradoException("id " + clienteId));

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ValidacionNegocioException("No se pueden crear credenciales para un cliente inactivo");
        }

        return crearNuevaSesion(cliente, usuario, passwordEncoder.encode(passwordPlano), null, null, null, null, null);
    }

    @Override
    @Transactional
    public LoginResponse autenticar(LoginRequest request) {
        Login ultimoLogin = loginRepository.findTopByUsuarioOrderByFechaInicioSesionDesc(request.getUsuario())
                .orElseThrow(() -> new ValidacionNegocioException("Usuario o contraseña incorrectos"));

        Cliente cliente = ultimoLogin.getCliente();
        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ValidacionNegocioException("El cliente está inactivo, no puede iniciar sesión");
        }

        if (!passwordEncoder.matches(request.getPassword(), ultimoLogin.getPasswordHash())) {
            throw new ValidacionNegocioException("Usuario o contraseña incorrectos");
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

    @Override
    @Transactional
    public boolean validarSesionActiva(String jwtToken) {
        Login login = loginRepository.findByJwtToken(jwtToken)
                .orElseThrow(() -> new ValidacionNegocioException("Sesión no encontrada"));

        if (!Boolean.TRUE.equals(login.getSesionActiva())) {
            return false;
        }

        LocalDateTime limite = login.getUltimaActividad().plusMinutes(login.getMinutosExpiracionInactividad());
        if (LocalDateTime.now().isAfter(limite)) {
            login.setSesionActiva(false);
            loginRepository.save(login);
            return false;
        }

        login.setUltimaActividad(LocalDateTime.now());
        loginRepository.save(login);
        return true;
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