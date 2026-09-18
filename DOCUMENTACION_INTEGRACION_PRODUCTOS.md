# Integración servicio externo de productos (GestoPago)

## Objetivo
Consumir `GET /sistema/service/getProductList.do` con autenticación Bearer Token,
reutilizando la integración con GestoPago que ya existía en el proyecto (mismo
proveedor externo, un endpoint adicional), siguiendo la arquitectura y convenciones
ya implementadas (Feign + capas Controller/Service/Client/DTO/Config).

**Estado:** integración probada de punta a punta contra el ambiente real de GestoPago.
Se validaron los tres escenarios principales: 200 con el catálogo completo, 401 sin
token vigente y 504 por timeout.

## Archivos agregados / modificados

| Capa | Archivo |
|---|---|
| Configuración Feign | `config/ProductoFeignConfig.java` |
| Cliente/Integración | `client/ProductoClient.java` |
| DTOs | `model/producto/ProductoDTO.java`, `ProductoServiceResponse.java`, `ProductoListResponse.java` |
| Excepciones | `exception/ProductoIntegrationException.java` (base), `ProductoAuthenticationException.java`, `ProductoTimeoutException.java`, `ProductoEmptyResponseException.java` |
| Servicio | `service/ProductoService.java`, `service/Impl/ProductoServiceImpl.java` |
| Controller | `controller/ProductoController.java`, `controller/ProductoExceptionHandler.java` |
| Pruebas | `src/test/java/.../services/ProductoServiceImplTest.java` |
| Build | `build.gradle` (dependencia `jackson-dataformat-xml`) |

## Decisiones técnicas

**1. El "nuevo servicio externo" es GestoPago, no un proveedor distinto.**
El path solicitado (`/sistema/service/getProductList.do`) sigue la misma convención
de nombres que el endpoint de autenticación ya existente (`/sistema/app/jwt-gp/authenticate/`),
ambos bajo `/sistema/...`. Por eso `ProductoClient` reutiliza `${gestopago.auth.url}`
como URL base, en vez de introducir una property nueva para un proveedor distinto.

**2. El Bearer token se obtiene de `GestoPagoTokenService`, no de una property estática.**
`ProductoFeignConfig` recibe `GestoPagoTokenService` por constructor (Spring lo inyecta
desde el contexto principal, aunque el `@FeignClient` cargue esta configuración en un
contexto hijo aislado) y en el `RequestInterceptor` consulta
`obtenerTokenActivo(idDistribuidor, codigoDispositivo)` para obtener el token JWT vigente
— el mismo que ya se renueva automáticamente cada hora (`@Scheduled` en
`GestoPagoTokenServiceImpl`) y se guarda en la tabla `gestopago_tokens`. El token nunca
queda hardcodeado ni se define como valor fijo en `application.properties`.

**3. `ProductoFeignConfig` sin la anotación `@Configuration`.**
Igual que antes: si se anotara, Spring la registraría de forma global y sus beans
(interceptor, timeouts) aplicarían a **todos** los FeignClients del proyecto, incluido
`GestoPagoAuthClient`. Al dejarla sin esa anotación y referenciarla solo desde
`@FeignClient(configuration = ProductoFeignConfig.class)`, Spring Cloud OpenFeign la
carga en un contexto hijo exclusivo para `ProductoClient`.

**4. Verificación de token antes de llamar (fail-fast).**
`ProductoServiceImpl` consulta `GestoPagoTokenService.obtenerTokenActivo(...)` **antes**
de invocar al cliente Feign. Si no hay token vigente, lanza `ProductoAuthenticationException`
de inmediato, sin gastar una llamada HTTP que se sabe que va a fallar.

**5. Formato XML: los datos vienen como atributos, no como elementos hijos.**
La respuesta de `getProductList.do` es XML, no JSON. Se agregó la dependencia
`jackson-dataformat-xml` y los DTOs (`ProductoDTO`, `ProductoServiceResponse`) usan
anotaciones de Jackson XML (`@JacksonXmlRootElement`, `@JacksonXmlProperty`,
`@JacksonXmlElementWrapper`).

El detalle clave, confirmado contra la respuesta real del servicio: cada producto llega
como un elemento `<producto>` cuyos datos son **atributos**, no elementos anidados. Por eso
cada campo de `ProductoDTO` lleva `@JacksonXmlProperty(isAttribute = true, ...)`. Sin ese
flag, Jackson busca elementos hijos, no los encuentra y deserializa todos los campos en
`null` — sin lanzar error, lo que hace el fallo difícil de diagnosticar. La única excepción
es `legend`, que sí viene como elemento hijo y por lo tanto va sin `isAttribute`.

Estructura real de la respuesta:

```xml
<RESPONSE>
  <PRODUCTOS>
    <producto servicio="TELCEL" producto="Telcel $50" idServicio="1" idProducto="101"
              idCatTipoServicio="1" tipoFront="TAE" hasDigitoVerificador="false"
              precio="50.00" showAyuda="true" tipoReferencia="TELEFONO">
      <legend>Recarga Telcel</legend>
    </producto>
    <!-- ... resto del catálogo ... -->
  </PRODUCTOS>
</RESPONSE>
```

El atributo XML se llama `producto` (igual que el elemento), por lo que en el DTO el campo
se llama `nombreProducto` y se mapea explícitamente con
`@JacksonXmlProperty(isAttribute = true, localName = "producto")`.

`ProductoListResponse` (la respuesta que expone nuestro propio `/productos`) sigue siendo
JSON — el cambio de formato solo afecta la comunicación con el servicio externo, no
nuestra propia API.

**6. Manejo de errores.**
En `ProductoServiceImpl.obtenerListaProductos()`, un único `try` con `catch` en cascada,
del más específico al más genérico:
- Sin token vigente en `GestoPagoTokenService` → `ProductoAuthenticationException` (verificación previa, sin llamar al cliente).
- `FeignException.Unauthorized` / `FeignException.Forbidden` → `ProductoAuthenticationException` (si el token expiró justo entre la verificación y la llamada, o el servidor lo rechaza igual).
- `RetryableException` (Feign la lanza tanto en timeout de conexión/lectura como en errores de comunicación) → `ProductoTimeoutException`.
- Cualquier otro `FeignException` (respuesta no exitosa, 4xx/5xx) → `ProductoIntegrationException`.
- Respuesta nula o `data` nula (valor nulo / sin respuesta) → `ProductoEmptyResponseException`.
- Cualquier otro error inesperado → `ProductoIntegrationException` genérica.

`ProductoExceptionHandler` (`@RestControllerAdvice(assignableTypes = ProductoController.class)`)
traduce esas excepciones a HTTP 401 / 504 / 502, con mensajes genéricos hacia el cliente;
el detalle técnico solo se registra en el log del servicio, nunca se expone en la respuesta.

**7. Logging.**
Se registra inicio y fin de la invocación (el "fin" va en un `finally`), y cada tipo de
error con su contexto, sin loguear nunca el token ni el header `Authorization`.

Durante la depuración se habilitó temporalmente el logging a nivel `DEBUG`/`full` del
cliente Feign, que imprime headers completos (incluido el Bearer token en texto plano).
Esa configuración se retiró una vez terminada la integración y no debe reactivarse fuera
de un entorno local de desarrollo.

**8. Inyección de dependencias.**
`ProductoServiceImpl` recibe `ProductoClient` y `GestoPagoTokenService` por constructor,
igual que el resto de servicios del proyecto — facilita las pruebas unitarias con Mockito.

## Configuración

No se agregaron properties nuevas para el servicio de productos: reutiliza las que ya
existían para GestoPago.

```properties
gestopago.auth.url=https://gestopago.portalventas.net
gestopago.auth.id-distribuidor=${GESTOPAGO_ID_DISTRIBUIDOR:}
gestopago.auth.codigo-dispositivo=${GESTOPAGO_CODIGO_DISPOSITIVO:}
gestopago.auth.password=${GESTOPAGO_PASSWORD:}
gestopago.auth.refresh-rate-ms=3600000

producto.service.connect-timeout-ms=${PRODUCTO_SERVICE_CONNECT_TIMEOUT_MS:5000}
producto.service.read-timeout-ms=${PRODUCTO_SERVICE_READ_TIMEOUT_MS:10000}
```

Las credenciales (password de GestoPago y del datasource) se leen de variables de entorno;
no deben quedar escritas en el archivo versionado.

Variables requeridas para levantar el servicio localmente:

| Variable | Descripción |
|---|---|
| `GESTOPAGO_ID_DISTRIBUIDOR` | Id de distribuidor asignado por GestoPago |
| `GESTOPAGO_CODIGO_DISPOSITIVO` | Código del dispositivo/TPV registrado |
| `GESTOPAGO_PASSWORD` | Password del dispositivo |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciales de Postgres local |

## Endpoint expuesto

**GET /productos**

Devuelve `ProductoListResponse` en JSON:

```json
{
  "codigo": 0,
  "mensaje": "Éxito",
  "productos": [
    {
      "servicio": "TELCEL",
      "nombreProducto": "Telcel $50",
      "idServicio": 1,
      "idProducto": 101,
      "idCatTipoServicio": 1,
      "tipoFront": "TAE",
      "hasDigitoVerificador": false,
      "precio": 50.00,
      "showAyuda": true,
      "tipoReferencia": "TELEFONO",
      "legend": "Recarga Telcel"
    }
  ]
}
```

Respuestas de error (`GenericResponse` con `codigo` y `mensaje`):

| Situación | HTTP |
|---|---|
| Sin token vigente / 401 / 403 del servicio externo | 401 |
| Timeout o error de comunicación | 504 |
| Respuesta vacía, o cualquier otro error de integración | 502 |

## Pruebas

`ProductoServiceImplTest` cubre, mockeando `ProductoClient` y `GestoPagoTokenService`
con Mockito:

- Respuesta exitosa con productos (verifica `codigo`, `mensaje` y el mapeo de los campos del DTO).
- Lista vacía: no se trata como error, devuelve `codigo: 0` con `productos: []`.
- Sin token vigente: además de la excepción, se verifica con `verify(..., never())` que no se llegó a invocar al cliente Feign.
- Respuesta nula (sin respuesta).
- `data` nula dentro de una respuesta no nula (valor nulo).
- Error de autenticación 401 desde el cliente Feign.
- Error de autorización 403 desde el cliente Feign.
- Timeout / error de comunicación.
- Respuesta no exitosa (500).
- Excepción inesperada no-Feign, que debe envolverse en `ProductoIntegrationException`.

Correr con: `./gradlew test`

## Pendientes / siguientes pasos sugeridos

- **Validar la vigencia del token, no solo su existencia.** `obtenerTokenActivo(...)` hoy
  devuelve el registro de `gestopago_tokens` sin filtrar por el campo `activo` ni por
  `expires_in`. Si el token está vencido, el fail-fast no lo detecta y la llamada sale
  igual para terminar en 401. Conviene guardar una `fecha_expiracion` calculada al renovar
  y filtrar por ella.
- **Prueba de deserialización del XML.** El comportamiento de `isAttribute = true` no está
  cubierto por ninguna prueba; un test que pase un XML de ejemplo por `XmlMapper` y verifique
  el mapeo evitaría regresiones silenciosas (campos en `null` sin error).
- **Prueba de integración con WireMock** para no depender del ambiente real de GestoPago
  al correr la suite en CI.
- **Cacheo del catálogo.** La respuesta trae cientos de productos y cambia con poca
  frecuencia; un caché con TTL corto reduciría la latencia y la carga sobre el proveedor.
- Se investigó también el catálogo real de Puntored (otro posible proveedor mencionado
  en el ejercicio) y se identificó que su endpoint equivalente real sería
  `POST /recharge/find-suppliers`, con un contrato de datos distinto — se descartó esa
  ruta porque el path y la convención de URL apuntaban más consistentemente a GestoPago.