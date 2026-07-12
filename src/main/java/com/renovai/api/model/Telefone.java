package com.renovai.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "telefones")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Telefone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "telefone_id")
    private Integer telefoneId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perfil_id")
    private Perfil perfil;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "ddd", length = 3)
    private String ddd;

    @Column(name = "telefone", length = 20)
    private String telefone;
}