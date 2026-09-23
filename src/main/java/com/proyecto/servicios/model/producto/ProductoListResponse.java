package com.proyecto.servicios.model.producto;

import com.proyecto.servicios.model.GenericResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.Map;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProductoListResponse extends GenericResponse {

    private Map<String, List<ProductoDTO>> productos;
}