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
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "estoques", uniqueConstraints = {
    @UniqueConstraint(name = "uq_cooperativa_material", columnNames = {"cooperativa_id", "material_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID estoqueId;

    @ManyToOne
    @JoinColumn(name = "cooperativa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_estoques_cooperativas"))
    private Cooperativa cooperativa;

    @ManyToOne
    @JoinColumn(name = "material_id", nullable = false, foreignKey = @ForeignKey(name = "fk_estoques_materiais"))
    private Material material;

    @Column(name = "quantidade_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal quantidadeKg = BigDecimal.ZERO;

    @Column(name = "data_atualizacao", nullable = false)
    @Builder.Default
    private LocalDateTime dataAtualizacao = LocalDateTime.now();
}
