package com.proyecto.servicios.model.producto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JacksonXmlRootElement(localName = "producto")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductoDTO {

    @JacksonXmlProperty(isAttribute = true, localName = "servicio")
    private String servicio;

    @JacksonXmlProperty(isAttribute = true, localName = "producto")
    private String nombreProducto;

    @JacksonXmlProperty(isAttribute = true, localName = "idServicio")
    private Integer idServicio;

    @JacksonXmlProperty(isAttribute = true, localName = "idProducto")
    private Integer idProducto;

    @JacksonXmlProperty(isAttribute = true, localName = "idCatTipoServicio")
    private Integer idCatTipoServicio;

    @JacksonXmlProperty(isAttribute = true, localName = "tipoFront")
    private String tipoFront;

    @JacksonXmlProperty(isAttribute = true, localName = "hasDigitoVerificador")
    private Boolean hasDigitoVerificador;

    @JacksonXmlProperty(isAttribute = true, localName = "precio")
    private BigDecimal precio;

    @JacksonXmlProperty(isAttribute = true, localName = "showAyuda")
    private Boolean showAyuda;

    @JacksonXmlProperty(isAttribute = true, localName = "tipoReferencia")
    private String tipoReferencia;

    @JacksonXmlProperty(localName = "legend")
    private String legend;
}