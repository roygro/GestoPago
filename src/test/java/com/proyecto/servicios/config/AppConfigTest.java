package com.proyecto.servicios.config;

import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.Request;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AppConfigTest {

    @Test
    @DisplayName("ClockConfig instancia reloj del sistema")
    void clockConfig_instanciaReloj() {
        ClockConfig config = new ClockConfig();
        Clock clock = config.clock();
        assertNotNull(clock);
    }

    @Test
    @DisplayName("SeguridadConfig provee un PasswordEncoder funcional")
    void seguridadConfig_passwordEncoder() {
        SeguridadConfig config = new SeguridadConfig();
        PasswordEncoder encoder = config.passwordEncoder();
        assertNotNull(encoder);
        String hash = encoder.encode("miPassword123");
        assertTrue(encoder.matches("miPassword123", hash));
    }

    @Test
    @DisplayName("RedisConfig configura RedisTemplate con serializers")
    void redisConfig_redisTemplate() {
        RedisConfig config = new RedisConfig();
        RedisConnectionFactory factory = mock(RedisConnectionFactory.class);
        RedisTemplate<String, String> template = config.redisTemplate(factory);
        assertNotNull(template);
        assertEquals(factory, template.getConnectionFactory());
    }

    @Test
    @DisplayName("GestoPagoTxFeignConfig agrega header Authorization Bearer cuando hay token activo")
    void gestoPagoTxFeignConfig_agregaToken() {
        GestoPagoTokenService tokenService = mock(GestoPagoTokenService.class);
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("token_secreto_tx");

        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(token));

        GestoPagoTxFeignConfig config = new GestoPagoTxFeignConfig(tokenService);
        ReflectionTestUtils.setField(config, "idDistribuidor", 83);
        ReflectionTestUtils.setField(config, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(config, "connectTimeoutMs", 4000L);
        ReflectionTestUtils.setField(config, "readTimeoutMs", 8000L);

        RequestInterceptor interceptor = config.gestoPagoTxAuthRequestInterceptor();
        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        Map<String, Collection<String>> headers = template.headers();
        assertTrue(headers.containsKey("Authorization"));
        assertTrue(headers.get("Authorization").contains("Bearer token_secreto_tx"));

        Request.Options options = config.gestoPagoTxRequestOptions();
        assertNotNull(options);
        assertEquals(4000L, options.connectTimeoutMillis());
        assertEquals(8000L, options.readTimeoutMillis());
    }

    @Test
    @DisplayName("ProductoFeignConfig agrega header Authorization Bearer cuando hay token activo")
    void productoFeignConfig_agregaToken() {
        GestoPagoTokenService tokenService = mock(GestoPagoTokenService.class);
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("token_producto_123");

        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(token));

        ProductoFeignConfig config = new ProductoFeignConfig(tokenService);
        ReflectionTestUtils.setField(config, "idDistribuidor", 83);
        ReflectionTestUtils.setField(config, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(config, "connectTimeoutMs", 3000L);
        ReflectionTestUtils.setField(config, "readTimeoutMs", 6000L);

        RequestInterceptor interceptor = config.productoAuthRequestInterceptor();
        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        Map<String, Collection<String>> headers = template.headers();
        assertTrue(headers.containsKey("Authorization"));
        assertTrue(headers.get("Authorization").contains("Bearer token_producto_123"));

        Request.Options options = config.productoRequestOptions();
        assertNotNull(options);
        assertEquals(3000L, options.connectTimeoutMillis());
        assertEquals(6000L, options.readTimeoutMillis());
    }

    @Test
    @DisplayName("OpenApi genera especificación OpenAPI y customizer")
    void openApiConfig() {
        OpenApi openApi = new OpenApi();
        io.swagger.v3.oas.models.OpenAPI api = openApi.openAPI();
        assertNotNull(api);
        assertNotNull(api.getInfo());

        org.springdoc.core.customizers.OpenApiCustomizer customizer = openApi.respuestasYSeguridadComunes();
        assertNotNull(customizer);

        io.swagger.v3.oas.models.OpenAPI mockApi = new io.swagger.v3.oas.models.OpenAPI();
        io.swagger.v3.oas.models.Paths paths = new io.swagger.v3.oas.models.Paths();
        io.swagger.v3.oas.models.PathItem pathItem = new io.swagger.v3.oas.models.PathItem();
        io.swagger.v3.oas.models.Operation op = new io.swagger.v3.oas.models.Operation();
        op.setResponses(new io.swagger.v3.oas.models.responses.ApiResponses());
        pathItem.setPost(op);
        paths.addPathItem("/clientes/buscar", pathItem);
        mockApi.setPaths(paths);

        customizer.customise(mockApi);
        assertNotNull(mockApi.getComponents());
    }

    @Test
    @DisplayName("SwaggerMontosConfig se instancia correctamente")
    void swaggerMontosConfig() {
        SwaggerMontosConfig config = new SwaggerMontosConfig();
        assertNotNull(config);
    }
}
