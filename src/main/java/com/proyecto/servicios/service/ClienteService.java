package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.ClienteActualizaRequest;
import com.proyecto.servicios.model.cliente.ClienteRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface ClienteService {

    ClienteResponse registrarCliente(ClienteRequest request);

    List<ClienteResponse> consultarTodos();

    ClienteResponse consultarPorId(Long id);

    ClienteResponse consultarPorCurp(String curp);

    ClienteResponse consultarPorRfc(String rfc);

    ClienteResponse consultarPorNumeroCuenta(String numeroCuenta);

    List<ClienteResponse> consultarActivos();

    List<ClienteResponse> consultarPorRangoFechas(LocalDateTime desde, LocalDateTime hasta);

    ClienteResponse actualizarCliente(Long id, ClienteActualizaRequest request);

    ClienteResponse consultarPorCorreo(String correo);

    void darDeBajaLogica(Long id);
}