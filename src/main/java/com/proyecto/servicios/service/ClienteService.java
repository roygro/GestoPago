package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.ClienteActualizaRequest;
import com.proyecto.servicios.model.cliente.ClienteBusquedaRequest;
import com.proyecto.servicios.model.cliente.ClienteRequest;
import com.proyecto.servicios.model.cliente.ClienteResponse;


import java.util.List;

public interface ClienteService {

    ClienteResponse registrarCliente(ClienteRequest request);

    List<ClienteResponse> buscarClientes(ClienteBusquedaRequest filtros);

    ClienteResponse actualizarCliente(ClienteActualizaRequest request);

    void darDeBajaLogica(Long id);
}