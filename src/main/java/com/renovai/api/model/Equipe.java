package com.renovai.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "equipes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Equipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "equipe_id")
    private Integer equipeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cooperativa_id", nullable = false)
    private Cooperativa cooperativa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gestor_id", nullable = false)
    private Funcionario gestor;

    @Column(name = "nome", nullable = false, length = 255)
    private String nome;

    @Column(name = "data_criacao")
    private LocalDateTime dataCriacao;

    @Column(name = "esta_ativa")
    private Boolean estaAtiva = true;

    @PrePersist
    public void prePersist() {
        this.dataCriacao = LocalDateTime.now();
    }
}
