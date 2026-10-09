package com.proyecto.servicios.entity.sf;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cat_estado_civil")
@Getter
@Setter
public class EstadoCivil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Column(name = "clave", nullable = false, length = 20, unique = true)
    private String clave;

    @Column(name = "descripcion", nullable = false, length = 50)
    private String descripcion;

    @Column(name = "activo", nullable = false)
    private Boolean activo;
}
