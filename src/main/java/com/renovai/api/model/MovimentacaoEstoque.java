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
import java.time.LocalDateTime;

@Entity
@Table(name = "movimentacoes_estoques")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MovimentacaoEstoque {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID movimentacaoId;

    @ManyToOne
    @JoinColumn(name = "estoque_id", nullable = false, foreignKey = @ForeignKey(name = "fk_movimentacoes_estoques"))
    private Estoque estoque;

    @ManyToOne
    @JoinColumn(name = "triagem_id", foreignKey = @ForeignKey(name = "fk_movimentacoes_triagens"))
    private Triagem triagem;

    @ManyToOne
    @JoinColumn(name = "item_id", foreignKey = @ForeignKey(name = "fk_movimentacoes_itens"))
    private Item item;

    @Column(name = "quantidade_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal quantidadeKg;

    @Column(name = "tipo_movimentacao", length = 10)
    private String tipoMovimentacao;

    @Column(name = "data_movimentacao", nullable = false)
    private LocalDateTime dataMovimentacao = LocalDateTime.now();
}