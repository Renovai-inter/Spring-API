package com.renovai.api.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Entity
@Table(name = "equipes_cooperados", uniqueConstraints = {
    @UniqueConstraint(name = "uq_equipe_cooperado", columnNames = {"equipe_id", "cooperado_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipeCooperado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID equipeCooperadoId;

    @ManyToOne
    @JoinColumn(name = "equipe_id", nullable = false, foreignKey = @ForeignKey(name = "fk_equipes_cooperados_equipes"))
    private Equipe equipe;

    @ManyToOne
    @JoinColumn(name = "cooperado_id", nullable = false, foreignKey = @ForeignKey(name = "fk_equipes_cooperados_funcionarios"))
    private Funcionario cooperado;
}