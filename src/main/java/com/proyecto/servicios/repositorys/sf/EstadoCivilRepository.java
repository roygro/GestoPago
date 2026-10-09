package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.EstadoCivil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EstadoCivilRepository extends JpaRepository<EstadoCivil, Short> {

    List<EstadoCivil> findByActivoTrueOrderByIdAsc();

    boolean existsByClaveAndActivoTrue(String clave);
}
