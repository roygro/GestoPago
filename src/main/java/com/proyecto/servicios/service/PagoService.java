package com.proyecto.servicios.service;

import com.proyecto.servicios.model.pago.HistorialPagoDTO;
import com.proyecto.servicios.model.pago.PagoRequest;
import com.proyecto.servicios.model.pago.PagoResponse;
import com.proyecto.servicios.model.pago.ValidarReferenciaRequest;
import com.proyecto.servicios.model.pago.ValidarReferenciaResponse;

import java.util.List;

public interface PagoService {

    /**
     * Procesa una transacción de pago de servicio o recarga según el flujo completo.
     */
    PagoResponse procesarPago(PagoRequest request);

    /**
     * Concilia una transacción cuyo resultado fue indeterminado por timeout o corte de red.
     */
    PagoResponse conciliarTransaccion(String upc);

    /**
     * Valida el formato y compatibilidad de una referencia antes de permitir el pago.
     */
    ValidarReferenciaResponse validarReferencia(ValidarReferenciaRequest request);

    /**
     * Consulta el historial de pagos asociados a las cuentas del cliente autenticado.
     */
    List<HistorialPagoDTO> consultarHistorial();
}
