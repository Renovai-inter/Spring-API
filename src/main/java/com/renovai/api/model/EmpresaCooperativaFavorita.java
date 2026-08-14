package com.renovai.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "empresa_cooperativas_favoritas", uniqueConstraints = {
    @UniqueConstraint(name = "uq_favorito",
                      columnNames = {"empresa_id", "cooperativa_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaCooperativaFavorita {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID favoritoId;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_favoritos_empresas"))
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "cooperativa_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_favoritos_cooperativas"))
    private Cooperativa cooperativa;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();
}