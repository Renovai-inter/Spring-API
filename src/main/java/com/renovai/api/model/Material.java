package com.renovai.api.model;
 
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;
 
@Entity
@Table(name = "materiais")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "material_id")
    private UUID materialId;
 
    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_materiais_categorias"))
    private CategoriaMaterial categoria;
 
    @ManyToOne
    @JoinColumn(name = "cooperativa_id",
            foreignKey = @ForeignKey(name = "fk_materiais_cooperativa"))
    private Cooperativa cooperativa;
 
    @Column(name = "preco_sugerido", precision = 10, scale = 2)
    private BigDecimal precoSugerido;
 
    @Column(name = "esta_disponivel", nullable = false)
    private Boolean estaDisponivel = true;
 
    @Column(name = "imagem_url", columnDefinition = "TEXT")
    private String imagemUrl;
}