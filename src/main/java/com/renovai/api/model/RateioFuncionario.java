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

@Entity
@Table(name = "rateios_funcionarios", uniqueConstraints = {
    @UniqueConstraint(name = "uq_rateio_cooperado", columnNames = {"rateio_id", "cooperado_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateioFuncionario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID rateioFuncionarioId;

    @ManyToOne
    @JoinColumn(name = "rateio_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rateios_func_rateios"))
    private Rateio rateio;

    @ManyToOne
    @JoinColumn(name = "cooperado_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rateios_func_cooperados"))
    private Funcionario cooperado;

    @Column(name = "valor_rateio", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorRateio;
}