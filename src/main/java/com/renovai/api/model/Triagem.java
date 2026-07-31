package com.renovai.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;
import java.math.BigDecimal;

@Entity
@Table(name = "triagens")
@PrimaryKeyJoinColumn(name = "evento_id")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Triagem extends EventoOperacional {

    @ManyToOne
    @JoinColumn(name = "equipe_id", nullable = false, foreignKey = @ForeignKey(name = "fk_triagens_equipes"))
    private Equipe equipe;

    @ManyToOne
    @JoinColumn(name = "coleta_id", nullable = false, foreignKey = @ForeignKey(name = "fk_triagens_coletas"))
    private Coleta coleta;

    @ManyToOne
    @JoinColumn(name = "material_id", nullable = false, foreignKey = @ForeignKey(name = "fk_triagens_materiais"))
    private Material material;

    @Column(name = "quantidade_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal quantidadeKg;

    @Column(name = "quantidade_rejeito_kg", precision = 10, scale = 3)
    private BigDecimal quantidadeRejeitoKg = BigDecimal.ZERO;
}