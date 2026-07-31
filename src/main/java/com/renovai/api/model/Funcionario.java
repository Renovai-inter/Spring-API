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
import java.time.LocalDateTime;

@Entity
@Table(name = "funcionarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID funcionarioId;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_funcionarios_usuarios"))
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "cooperativa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_funcionarios_cooperativas"))
    private Cooperativa cooperativa;

    @ManyToOne
    @JoinColumn(name = "cargo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_funcionarios_cargos"))
    private Cargo cargo;

    @Column(name = "data_admissao", nullable = false)
    private LocalDateTime dataAdmissao = LocalDateTime.now();

    @Column(name = "esta_ativo", nullable = false)
    private Boolean estaAtivo = true;
}