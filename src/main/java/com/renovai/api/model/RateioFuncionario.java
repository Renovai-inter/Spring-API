package com.renovai.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "rateios_funcionarios", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"rateio_id", "cooperado_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RateioFuncionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rateio_funcionario_id")
    private Integer rateioFuncionarioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rateio_id", nullable = false)
    private Rateio rateio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cooperado_id", nullable = false)
    private Funcionario cooperado;

    @Column(name = "valor_rateio", precision = 10, scale = 2)
    private BigDecimal valorRateio;
}