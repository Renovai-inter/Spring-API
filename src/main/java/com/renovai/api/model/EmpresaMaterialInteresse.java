package com.renovai.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "empresa_materiais_interesses", uniqueConstraints = {
    @UniqueConstraint(name = "uq_empresa_categoria",
                      columnNames = {"empresa_id", "categoria_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpresaMaterialInteresse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID empresaMaterialId;

    @ManyToOne
    @JoinColumn(name = "empresa_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_emp_mat_empresas"))
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_emp_mat_categorias"))
    private CategoriaMaterial categoria;
}