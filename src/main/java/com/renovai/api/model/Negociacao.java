package com.renovai.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "negociacoes", uniqueConstraints = {
    @UniqueConstraint(name = "uq_pedido_cooperativa_negociacao",
                      columnNames = {"pedido_id", "cooperativa_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Negociacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID negociacaoId;

    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_negociacoes_pedidos"))
    private Pedido pedido;

    @ManyToOne
    @JoinColumn(name = "cooperativa_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_negociacoes_cooperativas"))
    private Cooperativa cooperativa;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_negociacoes_empresas"))
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "status_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_negociacoes_status"))
    private Status status;

    @Column(name = "valor_total", precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Column(name = "data_inicio", nullable = false)
    private LocalDateTime dataInicio = LocalDateTime.now();

    @Column(name = "data_fechamento")
    private LocalDateTime dataFechamento;
}