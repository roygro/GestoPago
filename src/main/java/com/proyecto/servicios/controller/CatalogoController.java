package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.cliente.EstadoCivilResponse;
import com.proyecto.servicios.model.cliente.NacionalidadResponse;
import com.proyecto.servicios.service.CatalogoService;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
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
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = EstadoCivilResponse.class))))
    })
    @GetMapping(value = "/estado-civil", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EstadoCivilResponse>> consultarEstadosCiviles() {
        return ResponseEntity.ok(catalogoService.consultarEstadosCiviles());
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(mediaType = "application/json",
                            array = @ArraySchema(schema = @Schema(implementation = NacionalidadResponse.class))))
    })
    @GetMapping(value = "/nacionalidades", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<NacionalidadResponse>> consultarNacionalidades() {
        return ResponseEntity.ok(catalogoService.consultarNacionalidades());
    }
}
