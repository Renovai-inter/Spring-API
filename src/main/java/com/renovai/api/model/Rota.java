package com.renovai.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Table(name = "rotas")
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rota {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID rotaId;

    @ManyToOne
    @JoinColumn(name = "cooperativa_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_rotas_cooperativas"))
    private Cooperativa cooperativa;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(name = "esta_ativa", nullable = false)
    private Boolean estaAtiva = true;
}