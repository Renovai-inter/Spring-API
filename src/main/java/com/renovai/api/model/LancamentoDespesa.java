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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "lancamentos_despesas", uniqueConstraints = {
    @UniqueConstraint(name = "uq_despesa_mes", columnNames = {"despesa_id", "mes_referencia"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LancamentoDespesa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID lancamentoId;

    @ManyToOne
    @JoinColumn(name = "despesa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_lancamentos_despesas_despesa"))
    private Despesa despesa;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "mes_referencia", nullable = false)
    private LocalDate mesReferencia;

    @Column(name = "data_lancamento", nullable = false)
    private LocalDateTime dataLancamento = LocalDateTime.now();
}