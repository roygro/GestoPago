# Integración servicio externo de productos (GestoPago)

## Objetivo
Consumir `GET /sistema/service/getProductList.do` con autenticación Bearer Token,
reutilizando la integración con GestoPago que ya existía en el proyecto (mismo
proveedor externo, un endpoint adicional), siguiendo la arquitectura y convenciones
ya implementadas (Feign + capas Controller/Service/Client/DTO/Config).

**Estado:** integración probada de punta a punta contra el ambiente real de GestoPago.
Se validaron los escenarios principales: 200 con el catálogo completo, 401 sin
token vigente y 504 por timeout. El catálogo se cachea en Redis con respaldo en base de datos.

## Archivos agregados / modificados

| Capa | Archivo |
|---|---|
| Configuración Feign | `config/ProductoFeignConfig.java` |
| Configuración Redis | `config/RedisConfig.java` |
| Cliente/Integración | `client/ProductoClient.java` |
| DTOs | `model/producto/ProductoDTO.java`, `ProductoServiceResponse.java`, `ProductoListResponse.java` |
| Excepciones | `exception/ProductoIntegrationException.java` (base), `ProductoAuthenticationException.java`, `ProductoTimeoutException.java`, `ProductoEmptyResponseException.java` |
| Manejo de errores | `exception/GlobalExceptionHandler.java` (sección "Servicio de productos") |
| Servicio | `service/ProductoService.java`, `service/Impl/ProductoServiceImpl.java` |
| Caché | `service/Impl/ProductoCacheService.java`, `repositorys/producto/ProductoCacheRepository.java`, `entity/producto/ProductoCache.java` |
| Controller | `controller/ProductoController.java` |
| Migración | `db/migration/V2__create_productos_cache.sql` |
| Pruebas | `ProductoServiceImplTest.java`, `ProductoCacheServiceTest.java` |
| Build | `build.gradle` (`jackson-dataformat-xml`, `spring-boot-starter-data-redis`, `spring-boot-starter-cache`) |

## Decisiones técnicas

**1. El "nuevo servicio externo" es GestoPago, no un proveedor distinto.**
El path solicitado (`/sistema/service/getProductList.do`) sigue la misma convención
de nombres que el endpoint de autenticación ya existente (`/sistema/app/jwt-gp/authenticate/`),
ambos bajo `/sistema/...`. Por eso `ProductoClient` reutiliza `${gestopago.auth.url}`
como URL base, en vez de introducir una property nueva para un proveedor distinto.

**2. El Bearer token se obtiene de `GestoPagoTokenService`, no de una property estática.**
`ProductoFeignConfig` recibe `GestoPagoTokenService` por constructor y en el
`RequestInterceptor` consulta `obtenerTokenActivo(idDistribuidor, codigoDispositivo)`
para obtener el token JWT vigente — el mismo que ya se renueva automáticamente cada hora
(`@Scheduled` en `GestoPagoTokenServiceImpl`) y se guarda en la tabla `gestopago_tokens`.
El token nunca queda hardcodeado.

**3. `ProductoFeignConfig` sin la anotación `@Configuration`.**
Si se anotara, Spring la registraría de forma global y sus beans (interceptor, timeouts)
aplicarían a **todos** los FeignClients del proyecto, incluido `GestoPagoAuthClient`.
Al dejarla sin esa anotación y referenciarla solo desde
`@FeignClient(configuration = ProductoFeignConfig.class)`, se carga en un contexto hijo
exclusivo para `ProductoClient`.

**4. Verificación de token antes de llamar (fail-fast).**
Antes de invocar al cliente Feign, `ProductoServiceImpl` consulta
`GestoPagoTokenService.obtenerTokenActivo(...)`. Si no hay token, lanza
`ProductoAuthenticationException` de inmediato, sin gastar una llamada HTTP que se sabe
que va a fallar.

**5. Formato XML: los datos vienen como atributos, no como elementos hijos.**
La respuesta de `getProductList.do` es XML. Se agregó `jackson-dataformat-xml` y los DTOs
usan anotaciones de Jackson XML. Cada producto llega como un elemento `<producto>` cuyos
datos son **atributos**, por eso cada campo de `ProductoDTO` lleva
`@JacksonXmlProperty(isAttribute = true, ...)`. Sin ese flag, Jackson deserializa todos los
campos en `null` sin lanzar error. La única excepción es `legend`, que viene como elemento hijo.

```xml
<RESPONSE>
  <PRODUCTOS>
    <producto servicio="TELCEL" producto="Telcel $50" idServicio="1" idProducto="101"
              idCatTipoServicio="1" tipoFront="TAE" hasDigitoVerificador="false"
              precio="50.00" showAyuda="true" tipoReferencia="TELEFONO">
      <legend>Recarga Telcel</legend>
    </producto>
  </PRODUCTOS>
</RESPONSE>
```

El atributo XML se llama `producto`, por lo que en el DTO el campo se llama
`nombreProducto` y se mapea con `@JacksonXmlProperty(isAttribute = true, localName = "producto")`.
El cambio de formato solo afecta la comunicación con el servicio externo; nuestra API sigue en JSON.

**6. Agrupación por tipo.**
Los productos se agrupan por `tipoFront` (`Map<String, List<ProductoDTO>>`, preservando
el orden de llegada con `LinkedHashMap`). Esa es la estructura que se cachea y se devuelve.

**7. Caché en dos niveles (Redis + base de datos).**
`ProductoCacheService` implementa la estrategia:
- **Lectura:** primero Redis (llave `productos:lista`). Si Redis no está disponible o no
  tiene el dato, se lee la tabla `productos_cache` y solo se usa si fue actualizada
  dentro del TTL.
- **Escritura:** se guarda en Redis con TTL de 1 hora. Si Redis falla, se guarda en
  la tabla `productos_cache` como respaldo.
- **Refresco programado:** `@Scheduled(cron = "0 0 6 * * *")` en `ProductoServiceImpl`
  fuerza una llamada real a GestoPago todos los días a las 6:00 am y actualiza el caché,
  para que el primer usuario del día no espere la llamada externa.
- Si la respuesta viene del caché, el mensaje es `"Éxito (Caché)"`.

**8. Manejo de errores.**
En `ProductoServiceImpl.obtenerListaProductos()`, un único `try` con `catch` en cascada,
del más específico al más genérico:
- Sin token vigente → `ProductoAuthenticationException` (verificación previa, sin llamar al cliente).
- `FeignException.Unauthorized` / `Forbidden` → `ProductoAuthenticationException`.
- `RetryableException` (timeout o error de comunicación) → `ProductoTimeoutException`.
- Cualquier otro `FeignException` → `ProductoIntegrationException`.
- Respuesta nula o `data` nula → `ProductoEmptyResponseException`.
- Cualquier otro error inesperado → `ProductoIntegrationException` genérica.

Estas excepciones se traducen a HTTP en el `GlobalExceptionHandler` (`@RestControllerAdvice`
único del proyecto), con mensajes genéricos hacia el cliente. El detalle técnico solo
se registra en el log, nunca se expone en la respuesta.

**9. Logging.**
Se registra inicio y fin de la invocación (el "fin" en un `finally`) y cada tipo de
error con su contexto, sin loguear nunca el token ni el header `Authorization`.
Durante la depuración se habilitó temporalmente el logging `DEBUG`/`full` del cliente
Feign, que imprime el Bearer token en texto plano; esa configuración se retiró y no
debe reactivarse fuera de un entorno local.

**10. Inyección de dependencias.**
`ProductoServiceImpl` recibe `ProductoClient`, `GestoPagoTokenService` y
`ProductoCacheService` por constructor, lo que facilita las pruebas con Mockito.

## Configuración

```properties
gestopago.auth.url=https://gestopago.portalventas.net
gestopago.auth.id-distribuidor=${GESTOPAGO_ID_DISTRIBUIDOR:}
gestopago.auth.codigo-dispositivo=${GESTOPAGO_CODIGO_DISPOSITIVO:}
gestopago.auth.password=${GESTOPAGO_PASSWORD:}
gestopago.auth.refresh-rate-ms=3600000

producto.service.connect-timeout-ms=${PRODUCTO_SERVICE_CONNECT_TIMEOUT_MS:5000}
producto.service.read-timeout-ms=${PRODUCTO_SERVICE_READ_TIMEOUT_MS:10000}

spring.data.redis.host=${REDIS_HOST:localhost}
spring.data.redis.port=${REDIS_PORT:6379}
```

Las credenciales se leen de variables de entorno y no deben quedar escritas en el
archivo versionado.

| Variable | Descripción |
|---|---|
| `GESTOPAGO_ID_DISTRIBUIDOR` | Id de distribuidor asignado por GestoPago |
| `GESTOPAGO_CODIGO_DISPOSITIVO` | Código del dispositivo/TPV registrado |
| `GESTOPAGO_PASSWORD` | Password del dispositivo |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Credenciales de Postgres |
| `REDIS_HOST` / `REDIS_PORT` | Redis (opcional: si no está disponible se usa la BD como respaldo) |

## Endpoint expuesto

**GET /productos** (requiere sesión: header `Authorization: Bearer <jwtToken>`)

Respuesta exitosa, envuelta en `RespuestaApi`, con los productos agrupados por `tipoFront`:

```json
{
  "codigo": 200,
  "mensaje": "Productos consultados correctamente",
  "datos": {
    "TAE": [
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
  },
  "timestamp": "2026-10-21T12:00:00"
}
```

Respuestas de error (`ErrorResponse` con `codigo`, `mensaje` y `timestamp`):

| Situación | HTTP |
|---|---|
| Sin sesión o JWT inválido/expirado | 401 |
| Sin token vigente / 401 / 403 del servicio externo | 401 |
| Timeout o error de comunicación | 504 |
| Respuesta vacía, o cualquier otro error de integración | 502 |

## Pruebas

`ProductoServiceImplTest` (mockeando `ProductoClient`, `GestoPagoTokenService` y
`ProductoCacheService`) cubre:
- Respuesta exitosa con productos y mapeo de campos del DTO.
- Lista vacía: no es error, devuelve el mapa vacío.
- Sin token vigente: se verifica con `verify(..., never())` que no se invocó al cliente Feign.
- Respuesta nula y `data` nula.
- Errores 401 y 403 desde el cliente Feign.
- Timeout / error de comunicación.
- Respuesta no exitosa (500).
- Excepción inesperada no-Feign envuelta en `ProductoIntegrationException`.

`ProductoCacheServiceTest` cubre la lectura/escritura en Redis y el respaldo en BD.

Correr con: `./gradlew test`

## Pendientes / siguientes pasos sugeridos

- **Validar la vigencia del token, no solo su existencia.** `obtenerTokenActivo(...)` hoy
  devuelve el registro de `gestopago_tokens` sin filtrar por `activo` ni por `expires_in`.
  Si el token está vencido, el fail-fast no lo detecta y la llamada termina en 401.
  Conviene guardar una `fecha_expiracion` al renovar y filtrar por ella.
- **Prueba de deserialización del XML.** Un test que pase un XML de ejemplo por `XmlMapper`
  y verifique el mapeo evitaría regresiones silenciosas (campos en `null` sin error).
- **Prueba de integración con WireMock** para no depender del ambiente real de GestoPago en CI.
- Se investigó también el catálogo de Puntored (`POST /recharge/find-suppliers`), pero se
  descartó porque el path y la convención de URL apuntaban más consistentemente a GestoPago.