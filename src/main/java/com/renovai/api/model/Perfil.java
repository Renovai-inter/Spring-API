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
@Table(name = "perfis")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID perfilId;

    @ManyToOne
    @JoinColumn(name = "empresa_id", foreignKey = @ForeignKey(name = "fk_perfis_empresas"))
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "cooperativa_id", foreignKey = @ForeignKey(name = "fk_perfis_cooperativas"))
    private Cooperativa cooperativa;

    @ManyToOne
    @JoinColumn(name = "endereco_id", foreignKey = @ForeignKey(name = "fk_perfis_enderecos"))
    private Endereco endereco;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, unique = true, length = 18)
    private String cnpj;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao = LocalDateTime.now();

    @Column(name = "esta_ativo", nullable = false)
    private Boolean estaAtivo = true;
}