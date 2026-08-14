package com.renovai.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "negociacao_itens", uniqueConstraints = {
    @UniqueConstraint(name = "uq_negociacao_item_material",
                      columnNames = {"negociacao_id", "material_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NegociacaoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID negociacaoItemId;

    @ManyToOne
    @JoinColumn(name = "negociacao_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_neg_itens_negociacao"))
    private Negociacao negociacao;

    @ManyToOne
    @JoinColumn(name = "material_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_neg_itens_materiais"))
    private Material material;

    @Column(name = "quantidade_kg", precision = 10, scale = 3)
    private BigDecimal quantidadeKg;

    @Column(name = "preco_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precoUnitario;
}