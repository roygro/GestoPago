package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.exception.cliente.CuentaNoEncontradaException;
import com.proyecto.servicios.model.cliente.CuentaResponse;
import com.proyecto.servicios.model.cliente.SaldoResponse;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.service.CuentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CuentaServiceImpl implements CuentaService {

    @Autowired
    private CuentaRepository cuentaRepository;

    @Override
    public CuentaResponse consultarPorNumeroCuenta(String numeroCuenta) {
        return toCuentaResponse(buscarOFallar(numeroCuenta));
    }

    @Override
    public SaldoResponse consultarSaldo(String numeroCuenta) {
        Cuenta cuenta = buscarOFallar(numeroCuenta);
        SaldoResponse dto = new SaldoResponse();
        if (cuenta.getSaldo() != null) {
            dto.setSaldoDisponible(cuenta.getSaldo().getSaldoDisponible());
            dto.setFechaActualizacion(cuenta.getSaldo().getFechaActualizacion());
        }
        return dto;
    }

    @Override
    public List<CuentaResponse> consultarActivas() {
        return cuentaRepository.findByEstatus("ACTIVA").stream()
                .map(this::toCuentaResponse).collect(Collectors.toList());
    }

    private Cuenta buscarOFallar(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
    }

    private CuentaResponse toCuentaResponse(Cuenta cuenta) {
        CuentaResponse dto = new CuentaResponse();
        dto.setId(cuenta.getId());
        dto.setNumeroCuenta(cuenta.getNumeroCuenta());
        dto.setFechaApertura(cuenta.getFechaApertura());
        dto.setEstatus(cuenta.getEstatus());
        if (cuenta.getSaldo() != null) {
            SaldoResponse saldoDto = new SaldoResponse();
            saldoDto.setSaldoDisponible(cuenta.getSaldo().getSaldoDisponible());
            saldoDto.setFechaActualizacion(cuenta.getSaldo().getFechaActualizacion());
            dto.setSaldo(saldoDto);
        }
        return dto;
    }
}