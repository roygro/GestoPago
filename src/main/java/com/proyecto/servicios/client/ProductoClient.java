package com.proyecto.servicios.client;

import com.proyecto.servicios.config.ProductoFeignConfig;
import com.proyecto.servicios.model.producto.ProductoServiceResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
        name = "productoClient",
        url = "${gestopago.auth.url}",
        configuration = ProductoFeignConfig.class
)
public interface ProductoClient {

    @GetMapping("/sistema/service/getProductList.do")
    ProductoServiceResponse getProductList();
}