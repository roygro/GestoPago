package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.proyecto.servicios.client.GestoPagoSendTxClient;
import com.proyecto.servicios.entity.pago.TransaccionPago;
import com.proyecto.servicios.entity.sf.Cliente;
import com.proyecto.servicios.entity.sf.Cuenta;
import com.proyecto.servicios.entity.sf.Saldo;
import com.proyecto.servicios.exception.PagoFallidoException;
import com.proyecto.servicios.exception.cliente.AccesoDenegadoException;
import com.proyecto.servicios.exception.cliente.CuentaNoEncontradaException;
import com.proyecto.servicios.exception.cliente.ValidacionNegocioException;
import com.proyecto.servicios.model.pago.*;
import com.proyecto.servicios.model.producto.ProductoDTO;
import com.proyecto.servicios.model.producto.ProductoListResponse;
import com.proyecto.servicios.repositorys.pago.TransaccionPagoRepository;
import com.proyecto.servicios.repositorys.sf.CuentaRepository;
import com.proyecto.servicios.repositorys.sf.SaldoRepository;
import com.proyecto.servicios.security.UsuarioAutenticado;
import com.proyecto.servicios.service.PagoService;
import com.proyecto.servicios.service.ProductoService;
import com.proyecto.servicios.service.TwoFactorService;
import com.proyecto.servicios.util.AesEncryptionHelper;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class PagoServiceImpl implements PagoService {

    private final CuentaRepository cuentaRepository;
    private final SaldoRepository saldoRepository;
    private final TransaccionPagoRepository transaccionPagoRepository;
    private final GestoPagoSendTxClient gestoPagoSendTxClient;
    private final UsuarioAutenticado usuarioAutenticado;
    private final TwoFactorService twoFactorService;
    private final ProductoService productoService;
    private final ObjectMapper objectMapper;
    private final XmlMapper xmlMapper;

    @Value("${gestopago.aes.secret-key}")
    private String secretKey;

    @Value("${gestopago.aes.iv}")
    private String iv;

    @Value("${gestopago.sendtx.unidad:083}")
    private String unidad;

    public PagoServiceImpl(CuentaRepository cuentaRepository,
                           SaldoRepository saldoRepository,
                           TransaccionPagoRepository transaccionPagoRepository,
                           GestoPagoSendTxClient gestoPagoSendTxClient,
                           UsuarioAutenticado usuarioAutenticado,
                           TwoFactorService twoFactorService,
                           ProductoService productoService) {
        this.cuentaRepository = cuentaRepository;
        this.saldoRepository = saldoRepository;
        this.transaccionPagoRepository = transaccionPagoRepository;
        this.gestoPagoSendTxClient = gestoPagoSendTxClient;
        this.usuarioAutenticado = usuarioAutenticado;
        this.twoFactorService = twoFactorService;
        this.productoService = productoService;
        this.objectMapper = new ObjectMapper();
        this.xmlMapper = new XmlMapper();
    }

    @Override
    @Transactional
    public PagoResponse procesarPago(PagoRequest request) {
        log.info("Iniciando procesamiento de pago. Cuenta={}, idProd={}, idSer={}, monto={}",
                request.getNumCuenta(), request.getIdProd(), request.getIdSer(), request.getMonto());

        // 1. Validar producto y monto contra el Catálogo de Servicios
        validarProductoYPrecio(request.getIdProd(), request.getIdSer(), request.getMonto());

        // 2. Control de idempotencia / Prevención de cargos duplicados en ventana corta (2 min)
        validarNoDuplicado(request);

        // 3. Validaciones de cuenta y cliente
        Cuenta cuenta = validarClienteActivo(request.getNumCuenta());

        // 4. Concurrencia segura: Bloqueo pesimista en Saldo (SELECT ... FOR UPDATE)
        Saldo saldo = validarSaldoConBloqueo(cuenta, request.getMonto());

        // 5. Validación de seguridad 2FA por correo
        twoFactorService.validarCodigo(request.getNumCuenta(), request.getTwoFactorCode());

        // 6. Preparar payload SendTx
        String upc = (request.getUpc() != null && !request.getUpc().isBlank())
                ? request.getUpc().trim()
                : generarUpcUnico();
        String horaLocal = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String telefono = (request.getTelefono() != null && !request.getTelefono().isBlank())
                ? request.getTelefono().trim()
                : "1111111111";

        SendTxPayloadDTO payloadDto = SendTxPayloadDTO.builder()
                .idProducto(request.getIdProd())
                .idServicio(request.getIdSer())
                .telefono(telefono)
                .horaLocal(horaLocal)
                .upc(upc)
                .unidad(unidad)
                .referencia(request.getReference() != null && !request.getReference().isBlank() ? request.getReference().trim() : null)
                .montoPago(request.getMonto().doubleValue())
                .build();

        // 7. Cifrado AES-256
        String signed;
        try {
            String jsonPayload = objectMapper.writeValueAsString(payloadDto);
            signed = AesEncryptionHelper.encrypt(jsonPayload, secretKey, iv);
        } catch (Exception e) {
            log.error("Error al serializar y cifrar la transacción: {}", e.getMessage(), e);
            throw new ValidacionNegocioException("Error al preparar la transacción cifrada: " + e.getMessage());
        }

        String random = System.currentTimeMillis() + "_rand";

        // 8. Registro previo de la transacción en estatus INICIADA para trazabilidad e idempotencia
        TransaccionPago txLocal = new TransaccionPago();
        txLocal.setNumeroCuenta(request.getNumCuenta());
        txLocal.setIdServicio(request.getIdSer());
        txLocal.setIdProducto(request.getIdProd());
        txLocal.setMonto(request.getMonto());
        txLocal.setTelefono(request.getTelefono());
        txLocal.setReferencia(request.getReference());
        txLocal.setUpc(upc);
        txLocal.setPayloadCifrado(signed);
        txLocal.setEstatus("INICIADA");
        transaccionPagoRepository.save(txLocal);

        // 9. Enviar transacción al proveedor externo con manejo de Timeout y Conciliación
        log.info("Enviando SenTx cifrado a GestoPago. UPC={}, random={}", upc, random);
        String xmlResponse;
        try {
            xmlResponse = gestoPagoSendTxClient.sendTx(random, signed);
            log.info("Respuesta de GestoPago recibida para UPC {}: {}", upc, xmlResponse);
        } catch (RetryableException ex) {
            log.error("Timeout al comunicarse con GestoPago para UPC {}: {}", upc, ex.getMessage());
            txLocal.setEstatus("PENDIENTE_CONCILIACION");
            txLocal.setMensajeRespuesta("Tiempo de espera agotado. Requiere verificación de estatus mediante conciliación.");
            transaccionPagoRepository.save(txLocal);

            throw new PagoFallidoException("El proveedor tardó en responder (Tiempo de espera agotado). "
                    + "Para proteger la consistencia financiera de su cuenta, el saldo NO ha sido debitado. "
                    + "La transacción quedó registrada con estatus PENDIENTE_CONCILIACION [UPC: " + upc
                    + "]. Puede verificar su estado con el endpoint de conciliación.");
        } catch (Exception e) {
            log.error("Error general de comunicación con GestoPago para UPC {}: {}", upc, e.getMessage(), e);
            txLocal.setEstatus("RECHAZADA");
            txLocal.setMensajeRespuesta("Fallo de comunicación: " + e.getMessage());
            transaccionPagoRepository.save(txLocal);
            throw new PagoFallidoException("Error al comunicarse con la pasarela de pagos: " + e.getMessage());
        }

        // 10. Procesar respuesta XML
        GestoPagoSendTxResponse txResponse;
        try {
            txResponse = xmlMapper.readValue(xmlResponse, GestoPagoSendTxResponse.class);
        } catch (Exception e) {
            log.error("Error al parsear la respuesta XML de GestoPago: {}. XML: {}", e.getMessage(), xmlResponse, e);
            txLocal.setEstatus("PENDIENTE_CONCILIACION");
            transaccionPagoRepository.save(txLocal);
            throw new PagoFallidoException("Respuesta ilegible de la pasarela de pagos. Requiere conciliación.");
        }

        // 11. Evaluar resultado: '01' = Éxito financiero
        if (txResponse.esExitoso()) {
            log.info("Transacción aprobada por GestoPago [01]. ID_TX={}, Autorización={}",
                    txResponse.getIdTx(), txResponse.getNumAutorizacion());

            BigDecimal saldoAnterior = saldo.getSaldoDisponible();
            BigDecimal nuevoSaldo = saldoAnterior.subtract(request.getMonto());

            saldo.setSaldoDisponible(nuevoSaldo);
            saldo.setFechaActualizacion(LocalDateTime.now());
            saldoRepository.save(saldo);

            twoFactorService.consumirCodigo(request.getNumCuenta());

            txLocal.setEstatus("EXITOSA");
            txLocal.setIdTxProveedor(txResponse.getIdTx());
            txLocal.setNumAutorizacion(txResponse.getNumAutorizacion());
            txLocal.setCodigoRespuesta(txResponse.getMensaje() != null ? txResponse.getMensaje().getCodigo() : "01");
            txLocal.setMensajeRespuesta(txResponse.getMensaje() != null ? txResponse.getMensaje().getTexto() : "Operación realizada con éxito");
            transaccionPagoRepository.save(txLocal);

            log.info("Saldo de la cuenta {} debitado exitosamente. Anterior={}, Nuevo={}",
                    request.getNumCuenta(), saldoAnterior, nuevoSaldo);

            return PagoResponse.builder()
                    .idTx(txResponse.getIdTx())
                    .numAutorizacion(txResponse.getNumAutorizacion())
                    .codigoRespuesta(txLocal.getCodigoRespuesta())
                    .mensajeRespuesta(txLocal.getMensajeRespuesta())
                    .numeroCuenta(request.getNumCuenta())
                    .montoPagado(request.getMonto())
                    .saldoAnterior(saldoAnterior)
                    .nuevoSaldo(nuevoSaldo)
                    .fechaTransaccion(txResponse.getFecha())
                    .upc(upc)
                    .build();
        } else {
            String codigo = (txResponse.getMensaje() != null && txResponse.getMensaje().getCodigo() != null)
                    ? txResponse.getMensaje().getCodigo()
                    : "UNKNOWN";
            String texto = (txResponse.getMensaje() != null && txResponse.getMensaje().getTexto() != null)
                    ? txResponse.getMensaje().getTexto()
                    : "Transacción rechazada por el proveedor";

            log.warn("Transacción rechazada por GestoPago. Código={}, Mensaje={}, ID_TX={}",
                    codigo, texto, txResponse.getIdTx());

            txLocal.setEstatus("RECHAZADA");
            txLocal.setIdTxProveedor(txResponse.getIdTx());
            txLocal.setCodigoRespuesta(codigo);
            txLocal.setMensajeRespuesta(texto);
            transaccionPagoRepository.save(txLocal);

            throw new PagoFallidoException(codigo, txResponse.getIdTx(), texto);
        }
    }

    @Override
    @Transactional
    public PagoResponse conciliarTransaccion(String upc) {
        TransaccionPago tx = transaccionPagoRepository.findByUpc(upc)
                .orElseThrow(() -> new ValidacionNegocioException("No se encontró la transacción con UPC: " + upc));

        if ("EXITOSA".equals(tx.getEstatus())) {
            return PagoResponse.builder()
                    .idTx(tx.getIdTxProveedor())
                    .numAutorizacion(tx.getNumAutorizacion())
                    .codigoRespuesta(tx.getCodigoRespuesta())
                    .mensajeRespuesta("La transacción ya se encuentra confirmada y aplicada")
                    .numeroCuenta(tx.getNumeroCuenta())
                    .montoPagado(tx.getMonto())
                    .saldoAnterior(BigDecimal.ZERO)
                    .nuevoSaldo(BigDecimal.ZERO)
                    .fechaTransaccion(tx.getFechaCreacion().toString())
                    .upc(tx.getUpc())
                    .build();
        }

        if (!"PENDIENTE_CONCILIACION".equals(tx.getEstatus())) {
            throw new ValidacionNegocioException("La transacción con UPC " + upc
                    + " se encuentra en estatus '" + tx.getEstatus() + "' y no requiere conciliación.");
        }

        log.info("Ejecutando confirmTx.do para conciliar transacción UPC={}", upc);
        String random = System.currentTimeMillis() + "_rand";
        String xmlConfirm;
        try {
            xmlConfirm = gestoPagoSendTxClient.confirmTx(random, tx.getPayloadCifrado());
            log.info("Respuesta confirmTx recibida: {}", xmlConfirm);
        } catch (Exception e) {
            log.error("Error al ejecutar confirmTx para UPC {}: {}", upc, e.getMessage(), e);
            throw new PagoFallidoException("No fue posible conciliar con el proveedor en este momento. Intente más tarde.");
        }

        GestoPagoConfirmTxResponse confirmResponse;
        try {
            confirmResponse = xmlMapper.readValue(xmlConfirm, GestoPagoConfirmTxResponse.class);
        } catch (Exception e) {
            throw new PagoFallidoException("Respuesta de confirmación no interpretable del proveedor.");
        }

        if (confirmResponse != null && confirmResponse.fueAplicada()) {
            log.info("Conciliación exitosa (Código 06). La transacción sí fue aplicada en GestoPago. UPC={}", upc);

            Cuenta cuenta = validarClienteActivo(tx.getNumeroCuenta());
            Saldo saldo = validarSaldoConBloqueo(cuenta, tx.getMonto());

            BigDecimal saldoAnterior = saldo.getSaldoDisponible();
            BigDecimal nuevoSaldo = saldoAnterior.subtract(tx.getMonto());
            saldo.setSaldoDisponible(nuevoSaldo);
            saldo.setFechaActualizacion(LocalDateTime.now());
            saldoRepository.save(saldo);

            twoFactorService.consumirCodigo(tx.getNumeroCuenta());

            tx.setEstatus("EXITOSA");
            tx.setNumAutorizacion(confirmResponse.getNumAutorizacion());
            tx.setIdTxProveedor(confirmResponse.getMensaje() != null ? confirmResponse.getMensaje().getIdTx() : null);
            tx.setCodigoRespuesta(confirmResponse.getMensaje() != null ? confirmResponse.getMensaje().getCodigo() : "06");
            tx.setMensajeRespuesta(confirmResponse.getMensaje() != null ? confirmResponse.getMensaje().getTexto() : "Transacción confirmada");
            transaccionPagoRepository.save(tx);

            return PagoResponse.builder()
                    .idTx(tx.getIdTxProveedor())
                    .numAutorizacion(tx.getNumAutorizacion())
                    .codigoRespuesta(tx.getCodigoRespuesta())
                    .mensajeRespuesta(tx.getMensajeRespuesta())
                    .numeroCuenta(tx.getNumeroCuenta())
                    .montoPagado(tx.getMonto())
                    .saldoAnterior(saldoAnterior)
                    .nuevoSaldo(nuevoSaldo)
                    .fechaTransaccion(LocalDateTime.now().toString())
                    .upc(tx.getUpc())
                    .build();
        } else {
            String texto = (confirmResponse != null && confirmResponse.getMensaje() != null)
                    ? confirmResponse.getMensaje().getTexto()
                    : "La transacción no fue aplicada por el proveedor";
            String codigo = (confirmResponse != null && confirmResponse.getMensaje() != null)
                    ? confirmResponse.getMensaje().getCodigo()
                    : "70";

            log.warn("Conciliación completada: La transacción no fue aplicada. UPC={}, Código={}", upc, codigo);
            tx.setEstatus("RECHAZADA");
            tx.setCodigoRespuesta(codigo);
            tx.setMensajeRespuesta(texto);
            transaccionPagoRepository.save(tx);

            throw new PagoFallidoException(codigo, "-1", "Conciliación finalizada: " + texto + ". No se realizó cargo a su cuenta.");
        }
    }

    /**
     * Valida la existencia del producto en el catálogo y que el monto solicitado respete las restricciones.
     */
    private void validarProductoYPrecio(Integer idProducto, Integer idServicio, BigDecimal monto) {
        try {
            ProductoListResponse catalogResponse = productoService.obtenerListaProductos();
            if (catalogResponse != null && catalogResponse.getProductos() != null) {
                ProductoDTO producto = catalogResponse.getProductos().values().stream()
                        .flatMap(List::stream)
                        .filter(p -> p.getIdProducto() != null && p.getIdProducto().equals(idProducto))
                        .findFirst()
                        .orElse(null);

                if (producto == null) {
                    throw new ValidacionNegocioException("El producto con ID " + idProducto + " no existe en el catálogo");
                }

                if (producto.getIdServicio() != null && !producto.getIdServicio().equals(idServicio)) {
                    throw new ValidacionNegocioException("El producto " + idProducto + " (" + producto.getNombreProducto()
                            + ") no pertenece al servicio indicado (" + idServicio + ")");
                }

                BigDecimal precioFijo = producto.getPrecio();
                if (precioFijo != null && precioFijo.compareTo(BigDecimal.ZERO) > 0) {
                    if (precioFijo.compareTo(monto) != 0) {
                        throw new ValidacionNegocioException("El monto solicitado ($" + monto
                                + ") no corresponde al precio fijo configurado para '"
                                + producto.getNombreProducto() + "' ($" + precioFijo + ")");
                    }
                }
            }
        } catch (ValidacionNegocioException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Catálogo no disponible para validación estricta de precios: {}", e.getMessage());
        }
    }

    /**
     * Previene solicitudes idénticas ejecutadas en menos de 2 minutos (doble clic / reintento erróneo).
     */
    private void validarNoDuplicado(PagoRequest request) {
        LocalDateTime haceDosMinutos = LocalDateTime.now().minusMinutes(2);
        boolean duplicada = transaccionPagoRepository.existeTransaccionRecienteDuplicada(
                request.getNumCuenta(),
                request.getIdSer(),
                request.getIdProd(),
                request.getMonto(),
                request.getReference(),
                request.getTelefono(),
                haceDosMinutos
        );

        if (duplicada) {
            throw new ValidacionNegocioException("Posible cargo duplicado: Ya se registró una transacción idéntica "
                    + "hace menos de 2 minutos para la cuenta " + request.getNumCuenta()
                    + ". Si requiere realizar otro pago, espere unos momentos.");
        }
    }

    private Cuenta validarClienteActivo(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));

        if (!"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            throw new ValidacionNegocioException("La cuenta " + numeroCuenta + " no se encuentra activa");
        }

        Cliente cliente = cuenta.getCliente();
        if (cliente == null || Boolean.FALSE.equals(cliente.getActivo())) {
            throw new ValidacionNegocioException("El cliente titular de la cuenta no se encuentra activo");
        }

        try {
            usuarioAutenticado.validarPropietario(cliente.getId());
        } catch (AccesoDenegadoException e) {
            if ("No tiene permiso para acceder a información de otro cliente".equals(e.getMessage())
                    || "No tiene permiso para acceder a informacin de otro cliente".equals(e.getMessage())) {
                throw e;
            }
        }

        return cuenta;
    }

    /**
     * Bloqueo pesimista sobre el saldo para garantizar integridad y evitar condiciones de carrera.
     */
    private Saldo validarSaldoConBloqueo(Cuenta cuenta, BigDecimal montoRequerido) {
        Saldo saldo = saldoRepository.findByCuenta_NumeroCuentaConBloqueo(cuenta.getNumeroCuenta())
                .orElse(cuenta.getSaldo());

        if (saldo == null || saldo.getSaldoDisponible() == null
                || saldo.getSaldoDisponible().compareTo(montoRequerido) < 0) {
            BigDecimal actual = saldo != null && saldo.getSaldoDisponible() != null
                    ? saldo.getSaldoDisponible()
                    : BigDecimal.ZERO;
            throw new ValidacionNegocioException("Saldo insuficiente en la cuenta "
                    + cuenta.getNumeroCuenta() + ". Saldo disponible: $" + actual + ", monto requerido: $" + montoRequerido);
        }
        return saldo;
    }

    private String generarUpcUnico() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}
