package com.renovai.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "empresas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "empresa_id")
    private UUID empresaId;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "imagem_url", columnDefinition = "TEXT")
    private String imagemUrl;

    @OneToMany(
        mappedBy = "empresa",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<EmpresaMaterialInteresse> materiaisInteresse;
}