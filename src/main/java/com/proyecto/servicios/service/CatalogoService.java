package com.proyecto.servicios.service;

import com.proyecto.servicios.model.cliente.EstadoCivilResponse;
import com.proyecto.servicios.model.cliente.NacionalidadResponse;

import java.util.List;

public interface CatalogoService {

    /** Estados civiles activos del catálogo, en el orden en que se dieron de alta. */
    List<EstadoCivilResponse> consultarEstadosCiviles();

    /** Nacionalidades activas del catálogo, ordenadas por país. */
    List<NacionalidadResponse> consultarNacionalidades();
}
