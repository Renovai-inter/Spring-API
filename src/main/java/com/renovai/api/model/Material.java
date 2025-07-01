package com.renovai.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "materiais")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_id")
    private Integer materialId;

    @Column(name = "categoria", nullable = false, length = 100)
    private String categoria;

    @Column(name = "preco_sugerido", precision = 10, scale = 2)
    private BigDecimal precoSugerido;

    @Column(name = "esta_disponivel")
    private Boolean estaDisponivel = true;
}
