package com.renovai.api.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "negociacao_mensagens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NegociacaoMensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID mensagemId;

    @ManyToOne
    @JoinColumn(name = "negociacao_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_neg_msg_negociacao"))
    private Negociacao negociacao;

    @ManyToOne
    @JoinColumn(name = "remetente_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_neg_msg_remetente"))
    private Perfil remetente;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String mensagem;

    @Column(name = "tipo_mensagem", length = 20)
    private String tipoMensagem = "TEXTO";   // "TEXTO" | "CONTRAPROPOSTA" | "SISTEMA"

    @Column(name = "data_envio", nullable = false)
    private LocalDateTime dataEnvio = LocalDateTime.now();
}