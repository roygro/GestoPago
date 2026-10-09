package com.proyecto.servicios.entity.sf;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cat_nacionalidad")
@Getter
@Setter
public class Nacionalidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    /** Gentilicio: Mexicano, Estadounidense... Es el valor que se manda y se guarda en clientes.nacionalidad. */
    @Column(name = "nacionalidad", nullable = false, length = 50, unique = true)
    private String nacionalidad;

    @Column(name = "pais", nullable = false, length = 60)
    private String pais;

    /** Código ISO 3166-1 alfa-2 (MX, US...). Solo informativo, no se expone en la API. */
    @Column(name = "codigo_pais", nullable = false, length = 2, unique = true)
    private String codigoPais;

    @Column(name = "activo", nullable = false)
    private Boolean activo;
}
