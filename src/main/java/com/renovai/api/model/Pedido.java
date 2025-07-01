package com.renovai.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pedido_id")
    private Integer pedidoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "data_pedido")
    private LocalDateTime dataPedido;

    @Column(name = "data_final")
    private LocalDateTime dataFinal;

    @PrePersist
    public void prePersist() {
        this.dataPedido = LocalDateTime.now();
    }
}
