package com.proyecto.servicios.repositorys.sf;

import com.proyecto.servicios.entity.sf.Nacionalidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NacionalidadRepository extends JpaRepository<Nacionalidad, Short> {

    List<Nacionalidad> findByActivoTrueOrderByPaisAsc();

    boolean existsByNacionalidadAndActivoTrue(String nacionalidad);
}
