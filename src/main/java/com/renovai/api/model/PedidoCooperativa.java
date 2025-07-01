package com.renovai.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pedidos_cooperativas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PedidoCooperativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pedido_cooperativa_id")
    private Integer pedidoCooperativaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cooperativa_id", nullable = false)
    private Cooperativa cooperativa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id", nullable = false)
    private Status status;
}
