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
@Table(name = "categorias_materiais")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoriaMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID categoriaId;

    @ManyToOne
    @JoinColumn(name = "categoria_pai_id", foreignKey = @ForeignKey(name = "fk_categorias_pai"))
    private CategoriaMaterial categoriaPai;

    @Column(name = "nome_categoria", nullable = false, length = 100)
    private String nomeCategoria;
}