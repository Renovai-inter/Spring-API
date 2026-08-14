package com.renovai.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cooperativas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cooperativa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID cooperativaId;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "numero_cooperados", nullable = false)
    private Integer numeroCooperados = 0;

    @Column(name = "horario_funcionamento", length = 100)
    private String horarioFuncionamento;

    @Column(name = "imagem_url", columnDefinition = "TEXT")
    private String imagemUrl;

    @Column(name = "contato_preferencial", length = 50)
    private String contatoPreferencial;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao = LocalDateTime.now();
}