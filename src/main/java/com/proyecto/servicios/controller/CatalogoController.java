package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.RespuestaApi;
import com.proyecto.servicios.model.cliente.EstadoCivilResponse;
import com.proyecto.servicios.model.cliente.NacionalidadResponse;
import com.proyecto.servicios.service.CatalogoService;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/catalogos")
public class CatalogoController {

    @Autowired
    private CatalogoService catalogoService;

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK")
    })
    @GetMapping(value = "/estado-civil", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<List<EstadoCivilResponse>>> consultarEstadosCiviles() {
        List<EstadoCivilResponse> estados = catalogoService.consultarEstadosCiviles();
        return ResponseEntity.ok(RespuestaApi.de(HttpStatus.OK,
                "Catálogo de estados civiles consultado correctamente", estados));
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK")
    })
    @GetMapping(value = "/nacionalidades", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaApi<List<NacionalidadResponse>>> consultarNacionalidades() {
        List<NacionalidadResponse> nacionalidades = catalogoService.consultarNacionalidades();
        return ResponseEntity.ok(RespuestaApi.de(HttpStatus.OK,
                "Catálogo de nacionalidades consultado correctamente", nacionalidades));
    }
}