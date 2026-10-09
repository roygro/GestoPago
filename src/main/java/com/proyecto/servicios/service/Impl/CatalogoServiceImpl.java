package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.model.cliente.EstadoCivilResponse;
import com.proyecto.servicios.model.cliente.NacionalidadResponse;
import com.proyecto.servicios.repositorys.sf.EstadoCivilRepository;
import com.proyecto.servicios.repositorys.sf.NacionalidadRepository;
import com.proyecto.servicios.service.CatalogoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CatalogoServiceImpl implements CatalogoService {

    @Autowired
    private EstadoCivilRepository estadoCivilRepository;

    @Autowired
    private NacionalidadRepository nacionalidadRepository;

    @Override
    public List<EstadoCivilResponse> consultarEstadosCiviles() {
        return estadoCivilRepository.findByActivoTrueOrderByIdAsc().stream()
                .map(e -> new EstadoCivilResponse(e.getClave(), e.getDescripcion()))
                .collect(Collectors.toList());
    }

    @Override
    public List<NacionalidadResponse> consultarNacionalidades() {
        return nacionalidadRepository.findByActivoTrueOrderByPaisAsc().stream()
                .map(n -> new NacionalidadResponse(n.getNacionalidad(), n.getPais()))
                .collect(Collectors.toList());
    }
}
