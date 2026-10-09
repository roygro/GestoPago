package com.proyecto.servicios.security;

import com.proyecto.servicios.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Apaga la bandera sesion_activa de las sesiones que ya vencieron por inactividad
 * (o cuyo JWT ya expiró), aunque el usuario no vuelva a llamar a la API.
 * Así la tabla login siempre refleja el estado real.
 */
@Component
@Slf4j
public class SesionLimpiezaScheduler {

    private final LoginService loginService;

    public SesionLimpiezaScheduler(LoginService loginService) {
        this.loginService = loginService;
    }

    @Scheduled(fixedRateString = "${sesion.limpieza-ms:60000}",
               initialDelayString = "${sesion.limpieza-ms:60000}")
    public void cerrarSesionesVencidas() {
        try {
            int cerradas = loginService.cerrarSesionesVencidas();
            if (cerradas > 0) {
                log.info("Sesiones cerradas por inactividad o expiración: {}", cerradas);
            }
        } catch (Exception e) {
            log.error("Error al cerrar sesiones vencidas: {}", e.getMessage(), e);
        }
    }
}
