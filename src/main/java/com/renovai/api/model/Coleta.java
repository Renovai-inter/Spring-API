package com.renovai.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coletas")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Coleta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coleta_id")
    private Integer coletaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cooperado_id", nullable = false)
    private Funcionario cooperado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_id")
    private Status status;

    @Column(name = "origem", length = 255)
    private String origem;

    @Column(name = "quantidade_kg", precision = 10, scale = 3)
    private BigDecimal quantidadeKg;

    @Column(name = "data_coleta")
    private LocalDateTime dataColeta;

    @PrePersist
    public void prePersist() {
        this.dataColeta = LocalDateTime.now();
    }
}
