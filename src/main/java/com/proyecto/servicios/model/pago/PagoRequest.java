package com.proyecto.servicios.model.pago;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Datos para procesar un pago de servicio o tiempo aire")
public class PagoRequest {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @JsonProperty("numCuenta")
    @JsonAlias({"NumCuenta", "numeroCuenta"})
    @Schema(description = "Número de cuenta origen (16 dígitos)", example = "1234567890123456")
    private String numCuenta;

    @NotNull(message = "El id de servicio es obligatorio")
    @JsonProperty("idSer")
    @JsonAlias({"IdSer", "idServicio"})
    @Schema(description = "Identificador del servicio en GestoPago", example = "133")
    private Integer idSer;

    @NotNull(message = "El id de producto es obligatorio")
    @JsonProperty("idProd")
    @JsonAlias({"IdProd", "idProducto"})
    @Schema(description = "Identificador del producto en GestoPago", example = "582")
    private Integer idProd;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    @JsonProperty("monto")
    @JsonAlias({"Monto", "montoPago"})
    @Schema(description = "Monto a pagar", example = "10.00")
    private BigDecimal monto;

    @JsonProperty("telefono")
    @JsonAlias({"Telefono"})
    @Schema(description = "Número telefónico (para recargas o identificador móvil)", example = "5555555551")
    private String telefono;

    @JsonProperty("reference")
    @JsonAlias({"Reference", "referencia"})
    @Schema(description = "Referencia del recibo o servicio", example = "25720381")
    private String reference;

    @NotBlank(message = "El código 2FA es obligatorio")
    @JsonProperty("2FA")
    @JsonAlias({"twoFactorCode", "twoFA", "codigo2fa", "2fa"})
    @Schema(description = "Código de doble factor de autenticación (OTP)", example = "123456")
    private String twoFactorCode;

    @JsonProperty("upc")
    @JsonAlias({"UPC", "Upc"})
    @Schema(description = "Código único de pago opcional (10 caracteres)", example = "7d3f6cb2b5")
    private String upc;
}
