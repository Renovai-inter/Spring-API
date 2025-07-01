package com.renovai.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rateios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Rateio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rateio_id")
    private Integer rateioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gestor_id", nullable = false)
    private Funcionario gestor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_rateio_id")
    private TipoRateio tipoRateio;

    @Column(name = "data_rateio")
    private LocalDateTime dataRateio;

    @PrePersist
    public void prePersist() {
        this.dataRateio = LocalDateTime.now();
    }
}
