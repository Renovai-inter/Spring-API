package com.renovai.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cooperativas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cooperativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cooperativa_id")
    private Integer cooperativaId;

    @Column(name = "nome", nullable = false, length = 255)
    private String nome;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "numero_cooperados")
    private Integer numeroCooperados;

    @Column(name = "horario_funcionamento", length = 100)
    private String horarioFuncionamento;
}
