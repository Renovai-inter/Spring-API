package com.renovai.api.model;

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

@Entity
@Table(name = "pedidos_cooperativas", uniqueConstraints = {
    @UniqueConstraint(name = "uq_pedido_cooperativa", columnNames = {"pedido_id", "cooperativa_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PedidoCooperativa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID pedidoCooperativaId;

    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pedidos_coop_pedidos"))
    private Pedido pedido;

    @ManyToOne
    @JoinColumn(name = "cooperativa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pedidos_coop_cooperativas"))
    private Cooperativa cooperativa;

    @ManyToOne
    @JoinColumn(name = "status_id", nullable = false, foreignKey = @ForeignKey(name = "fk_pedidos_coop_status"))
    private Status status;
}