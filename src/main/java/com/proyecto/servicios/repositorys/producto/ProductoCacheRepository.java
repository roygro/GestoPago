package com.proyecto.servicios.repositorys.producto;

import com.proyecto.servicios.entity.producto.ProductoCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductoCacheRepository extends JpaRepository<ProductoCache, Long> {

    Optional<ProductoCache> findByCacheKey(String cacheKey);
}