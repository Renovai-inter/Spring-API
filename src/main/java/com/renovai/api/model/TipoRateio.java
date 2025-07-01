package com.renovai.api.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tipos_rateios")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TipoRateio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tipo_rateio_id")
    private Integer tipoRateioId;

    @Column(name = "tipo_rateio", nullable = false, length = 255)
    private String tipoRateio;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;
}
