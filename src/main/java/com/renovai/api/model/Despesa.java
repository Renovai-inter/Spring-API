package com.renovai.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "despesas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Despesa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID despesaId;

    @ManyToOne
    @JoinColumn(name = "cooperativa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_despesas_cooperativa"))
    private Cooperativa cooperativa;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(name = "tipo_despesa", nullable = false, length = 10)
    private String tipoDespesa;

    @Column(name = "esta_ativa", nullable = false)
    private Boolean estaAtiva = true;
}