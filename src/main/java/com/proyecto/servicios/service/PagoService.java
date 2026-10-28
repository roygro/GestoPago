package com.proyecto.servicios.service;

import com.proyecto.servicios.model.pago.PagoRequest;
import com.proyecto.servicios.model.pago.PagoResponse;

public interface PagoService {

    /**
     * Procesa una transacción de pago de servicio o recarga según el flujo completo:
     * 1. Validar Cliente Activo.
     * 2. Validar Saldo suficiente.
     * 3. Validar código 2FA.
     * 4. Construir payload SendTx.
     * 5. Cifrar con AES-256 (AesEncryptionHelper).
     * 6. Obtener Bearer Token de GestoPago.
     * 7. Enviar la transacción cifrada.
     * 8. Si la respuesta es '01' (Pago Exitoso), debitar el saldo en BD y confirmar.
     */
    PagoResponse procesarPago(PagoRequest request);

    /**
     * Concilia una transacción cuyo resultado fue indeterminado por timeout o corte de red,
     * consumiendo el método confirmTx.do de GestoPago.
     */
    PagoResponse conciliarTransaccion(String upc);
}
