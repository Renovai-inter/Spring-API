package com.renovai.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "tipos_rateios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoRateio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID tipoRateioId;

    @Column(name = "tipo_rateio", nullable = false, unique = true, length = 255)
    private String tipoRateio;

    @Column(columnDefinition = "TEXT")
    private String descricao;
}