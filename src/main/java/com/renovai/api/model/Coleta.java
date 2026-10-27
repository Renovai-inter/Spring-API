package com.renovai.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
import java.math.BigDecimal;

@Entity
@Table(name = "coletas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Coleta extends EventoOperacional {

    @ManyToOne
    @JoinColumn(name = "cooperado_id", nullable = false, foreignKey = @ForeignKey(name = "fk_coletas_cooperados"))
    private Funcionario cooperado;

    @Column(name = "quantidade_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal quantidadeKg;

    @Column(name = "tipo_coleta", length = 30)
    private String tipoColeta;

    @Column(name = "imagem_url", columnDefinition = "TEXT")
    private String imagemUrl;

    @ManyToOne
    @JoinColumn(name = "rota_id", foreignKey = @ForeignKey(name = "fk_coletas_rotas"))
    private Rota rota;

}
