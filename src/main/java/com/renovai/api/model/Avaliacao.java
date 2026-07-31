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
import java.time.LocalDateTime;

@Entity
@Table(name = "avaliacoes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID avaliacaoId;

    @ManyToOne
    @JoinColumn(name = "avaliador_id", nullable = false, foreignKey = @ForeignKey(name = "fk_avaliacoes_avaliador"))
    private Perfil avaliador;

    @ManyToOne
    @JoinColumn(name = "avaliado_id", nullable = false, foreignKey = @ForeignKey(name = "fk_avaliacoes_avaliado"))
    private Perfil avaliado;

    @ManyToOne
    @JoinColumn(name = "pedido_id", nullable = false, foreignKey = @ForeignKey(name = "fk_avaliacoes_pedidos"))
    private Pedido pedido;

    @Column(nullable = true)
    private Integer nota;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @Column(name = "data_avaliacao", nullable = false)
    private LocalDateTime dataAvaliacao = LocalDateTime.now();
}