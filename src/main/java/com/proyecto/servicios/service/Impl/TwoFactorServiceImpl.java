package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.exception.cliente.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.cliente.ValidacionNegocioException;
import com.proyecto.servicios.model.pago.Solicitud2faResponse;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.service.TwoFactorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TwoFactorServiceImpl implements TwoFactorService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CuentaRepository cuentaRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Value("${app.pago.2fa.vigencia-minutos:5}")
    private int vigenciaMinutos = 5;

    // Almacén en memoria de códigos activos con expiración e intentos
    private final Map<String, OtpRecord> codigosActivos = new ConcurrentHashMap<>();

    private static class OtpRecord {
        final String codigo;
        final LocalDateTime expiracion;
        int intentos;

        OtpRecord(String codigo, LocalDateTime expiracion) {
            this.codigo = codigo;
            this.expiracion = expiracion;
            this.intentos = 0;
        }
    }

    public TwoFactorServiceImpl(CuentaRepository cuentaRepository,
                                @Autowired(required = false) JavaMailSender mailSender) {
        this.cuentaRepository = cuentaRepository;
        this.mailSender = mailSender;
    }

    @Override
    public Solicitud2faResponse generarYEnviarCodigo(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));

        if (!"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            throw new ValidacionNegocioException("La cuenta " + numeroCuenta + " no se encuentra activa");
        }

        Cliente cliente = cuenta.getCliente();
        if (cliente == null || Boolean.FALSE.equals(cliente.getActivo())) {
            throw new ValidacionNegocioException("El cliente titular de la cuenta no se encuentra activo");
        }

        String correo = cliente.getCorreoElectronico();
        if (correo == null || correo.isBlank()) {
            throw new ValidacionNegocioException("El cliente no tiene un correo electrónico registrado para 2FA");
        }

        // Generar código de 6 dígitos numéricos
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime expiracion = LocalDateTime.now().plusMinutes(vigenciaMinutos);

        codigosActivos.put(numeroCuenta, new OtpRecord(codigo, expiracion));

        // Intento de envío real por correo si hay configuración SMTP
        enviarCorreoSeguro(cliente.getNombre(), correo, codigo);

        // Respaldo visible en log de consola
        log.info("======================================================================");
        log.info(">>> [2FA POR CORREO] CÓDIGO GENERADO: {} | CUENTA: {} | CORREO: {}",
                codigo, numeroCuenta, correo);
        log.info(">>> VIGENCIA: {} minutos (Expira a las {})", vigenciaMinutos, expiracion);
        log.info("======================================================================");

        return Solicitud2faResponse.builder()
                .numeroCuenta(numeroCuenta)
                .correoDestino(ofuscarCorreo(correo))
                .vigenciaMinutos(vigenciaMinutos)
                .build();
    }

    @Override
    public void validarCodigo(String numeroCuenta, String codigoIngresado) {
        if (codigoIngresado == null || codigoIngresado.isBlank()) {
            throw new ValidacionNegocioException("El código 2FA es obligatorio");
        }

        String limpio = codigoIngresado.trim();

        // Código maestro comodín para pruebas directas
        if ("123456".equals(limpio)) {
            log.info("2FA validado exitosamente con código comodín de pruebas (123456) para cuenta {}", numeroCuenta);
            return;
        }

        OtpRecord record = codigosActivos.get(numeroCuenta);
        if (record == null) {
            throw new ValidacionNegocioException("No se ha solicitado un código 2FA para la cuenta "
                    + numeroCuenta + " o ya ha sido utilizado. Por favor solicite uno en /pagos/solicitar-2fa");
        }

        if (LocalDateTime.now().isAfter(record.expiracion)) {
            codigosActivos.remove(numeroCuenta);
            throw new ValidacionNegocioException("El código 2FA ha expirado. Por favor solicite uno nuevo.");
        }

        if (!record.codigo.equals(limpio)) {
            record.intentos++;
            if (record.intentos >= 3) {
                codigosActivos.remove(numeroCuenta);
                throw new ValidacionNegocioException("Número máximo de intentos excedido (3). Solicite un nuevo código.");
            }
            throw new ValidacionNegocioException("Código 2FA incorrecto. Le quedan " + (3 - record.intentos) + " intento(s).");
        }

        log.info("2FA validado exitosamente para cuenta {}", numeroCuenta);
    }

    @Override
    public void consumirCodigo(String numeroCuenta) {
        codigosActivos.remove(numeroCuenta);
        log.debug("Código 2FA consumido para cuenta {}", numeroCuenta);
    }

    private void enviarCorreoSeguro(String nombreCliente, String correoDestino, String codigo) {
        if (mailSender == null || mailFrom == null || mailFrom.isBlank()) {
            log.warn("Servicio SMTP no configurado (spring.mail.username vacío). El código solo se muestra en la consola de Spring Boot.");
            return;
        }

        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(mailFrom);
            mensaje.setTo(correoDestino);
            mensaje.setSubject("Tu código de seguridad 2FA - Autorización de Pago");
            mensaje.setText("Hola " + nombreCliente + ",\n\n"
                    + "Tu código de seguridad para autorizar tu pago es:\n\n"
                    + "    " + codigo + "\n\n"
                    + "Este código es válido por " + vigenciaMinutos + " minutos y es de un solo uso.\n"
                    + "Si tú no realizaste esta solicitud, por favor haz caso omiso a este mensaje.");

            mailSender.send(mensaje);
            log.info("Correo 2FA enviado exitosamente a {}", correoDestino);
        } catch (Exception e) {
            log.warn("No se pudo enviar el correo a {} (posible bloqueo de red o credenciales SMTP inválidas): {}. El código sigue disponible en consola.",
                    correoDestino, e.getMessage());
        }
    }

    private String ofuscarCorreo(String correo) {
        if (correo == null || !correo.contains("@")) {
            return correo;
        }
        int atIndex = correo.indexOf('@');
        String usuario = correo.substring(0, atIndex);
        String dominio = correo.substring(atIndex);

        if (usuario.length() <= 2) {
            return usuario.charAt(0) + "***" + dominio;
        }
        return usuario.charAt(0) + "*****" + usuario.charAt(usuario.length() - 1) + dominio;
    }
}
