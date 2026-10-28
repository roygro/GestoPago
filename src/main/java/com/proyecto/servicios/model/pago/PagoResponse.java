package com.proyecto.servicios.model.pago;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta de confirmación de pago")
public class PagoResponse {

    @Schema(description = "Identificador único de la transacción en GestoPago", example = "291437188")
    private String idTx;

    @Schema(description = "Número de autorización emitido por el proveedor", example = "100024988")
    private String numAutorizacion;

    @Schema(description = "Código de respuesta del proveedor (01 = Exitoso)", example = "01")
    private String codigoRespuesta;

    @Schema(description = "Mensaje emitido por el proveedor", example = "Operacion realizada con exito")
    private String mensajeRespuesta;

    @Schema(description = "Número de cuenta desde la cual se debitó el pago", example = "1234567890123456")
    private String numeroCuenta;

    @Schema(description = "Monto pagado", example = "10.00")
    private BigDecimal montoPagado;

    @Schema(description = "Saldo disponible previo al pago", example = "150.00")
    private BigDecimal saldoAnterior;

    @Schema(description = "Nuevo saldo disponible tras el débito", example = "140.00")
    private BigDecimal nuevoSaldo;

    @Schema(description = "Fecha y hora de la transacción devuelta por el proveedor", example = "25/Feb/2022 18:15:25")
    private String fechaTransaccion;

    @Schema(description = "Identificador único de pago enviado (UPC)", example = "UPC_Test_XX")
    private String upc;
}
