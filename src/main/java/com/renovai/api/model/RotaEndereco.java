package com.renovai.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "rotas_enderecos")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RotaEndereco {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID rotaEnderecoId;

    @ManyToOne
    @JoinColumn(name = "rota_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_rotas_end_rotas"))
    private Rota rota;

    @ManyToOne
    @JoinColumn(name = "endereco_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_rotas_end_enderecos"))
    private Endereco endereco;

    @Column(name = "nome_local", nullable = false, length = 255)
    private String nomeLocal;

    @Column(name = "tipo_local", length = 50)
    private String tipoLocal;

    @Column(nullable = false)
    private Integer ordem = 1;
}