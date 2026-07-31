package com.renovai.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
import java.math.BigDecimal;

@Entity
@Table(name = "materiais")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID materialId;

    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false, foreignKey = @ForeignKey(name = "fk_materiais_categorias"))
    private CategoriaMaterial categoria;

    @ManyToOne
    @JoinColumn(name = "cooperativa_id", foreignKey = @ForeignKey(name = "fk_materiais_cooperativa"))
    private Cooperativa cooperativa;

    @Column(name = "preco_sugerido", precision = 10, scale = 2)
    private BigDecimal precoSugerido;

    @Column(name = "esta_disponivel", nullable = false)
    private Boolean estaDisponivel = true;
}