package com.renovai.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "equipe_cooperados")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EquipeCooperado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "equipe_cooperado_id")
    private Integer equipeCooperadoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipe_id", nullable = false)
    private Equipe equipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cooperado_id", nullable = false)
    private Funcionario cooperado;
}