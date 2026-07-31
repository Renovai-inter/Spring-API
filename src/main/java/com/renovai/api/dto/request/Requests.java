package com.renovai.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class Requests {

    public record CooperativaRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 255)
        String nome,

        String descricao,

        @Min(value = 0, message = "Número de cooperados não pode ser negativo")
        Integer numeroCooperados,

        @Size(max = 100)
        String horarioFuncionamento
    ) {}

    public record EmpresaRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 255)
        String nome,

        String descricao,

        UUID materialId
    ) {}

    public record PreCadastroRequest(
        String nome,
        String cpf,
        String senhaTemporaria,
        UUID cargoId,

        @NotNull(message = "ID da cooperativa é obrigatório")
        UUID cooperativaId
    ) {}

    // Material agora referencia categoriaId (UUID) em vez de String categoria
    public record MaterialRequest(
        @NotNull(message = "ID da categoria é obrigatório")
        UUID categoriaId,

        UUID cooperativaId,

        @DecimalMin(value = "0.0", message = "Preço sugerido deve ser positivo")
        BigDecimal precoSugerido,

        Boolean estaDisponivel
    ) {}

    public record UsuarioRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 255)
        String nome,

        @Pattern(
            regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}",
            message = "CPF inválido (use 000.000.000-00)"
        )
        String cpf,

        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        String senha,

        LocalDate dataNascimento
    ) {}

    public record EnderecoRequest(
        @Pattern(
            regexp = "\\d{5}-\\d{3}",
            message = "CEP inválido (use 00000-000)"
        )
        String cep,

        @Size(max = 255)
        String logradouro,

        @Size(max = 20)
        String numero,

        @Size(max = 255)
        String complemento,

        @Size(max = 100)
        String bairro,

        @Size(max = 100)
        String cidade
    ) {}

    public record ValidarPrimeiroAcessoRequest(
        String cpf,
        String senha
    ) {}

    public record CompletarCadastroRequest(
        String cpf,
        String email,
        String novaSenha
    ) {}

    public record PerfilRequest(
        UUID empresaId,
        UUID cooperativaId,
        UUID enderecoId,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        @Pattern(
            regexp = "\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}",
            message = "CNPJ inválido"
        )
        String cnpj
    ) {}

    public record ColetaRequest(
        @NotNull(message = "ID do cooperado é obrigatório")
        UUID cooperadoId,

        UUID statusId,

        @Size(max = 255)
        String origem,

        @DecimalMin(
            value = "0.001",
            message = "Quantidade deve ser maior que zero"
        )
        BigDecimal quantidadeKg
    ) {}

    public record TriagemRequest(
        @NotNull(message = "ID da equipe é obrigatório")
        UUID equipeId,

        @NotNull(message = "ID da coleta é obrigatório")
        UUID coletaId,

        @NotNull(message = "ID do material é obrigatório")
        UUID materialId,

        UUID statusId,

        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(
            value = "0.001",
            message = "Quantidade deve ser maior que zero"
        )
        BigDecimal quantidadeKg,

        @DecimalMin(
            value = "0.0",
            message = "Quantidade de rejeito não pode ser negativa"
        )
        BigDecimal quantidadeRejeitoKg
    ) {}

    public record PedidoRequest(
        @NotNull(message = "ID da empresa é obrigatório")
        UUID empresaId
    ) {}

    public record ItemRequest(
        @NotNull(message = "ID do pedido é obrigatório")
        UUID pedidoId,

        @NotNull(message = "ID do material é obrigatório")
        UUID materialId,

        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(
            value = "0.001",
            message = "Quantidade deve ser maior que zero"
        )
        BigDecimal quantidadeKg,

        @DecimalMin(
            value = "0.0",
            message = "Preço unitário não pode ser negativo"
        )
        BigDecimal precoUnitario
    ) {}

    public record AvaliacaoRequest(
        @NotNull(message = "ID do avaliador é obrigatório")
        UUID avaliadorId,

        @NotNull(message = "ID do avaliado é obrigatório")
        UUID avaliadoId,

        UUID pedidoId,

        @Min(value = 1, message = "Nota mínima é 1")
        @Max(value = 5, message = "Nota máxima é 5")
        Integer nota,

        String comentario
    ) {}

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class RateioRequest {
        private UUID gestorId;
        private UUID tipoRateioId;
    }

    public record EstoqueRequest(
        @NotNull(message = "ID da cooperativa é obrigatório")
        UUID cooperativaId,

        @NotNull(message = "ID do material é obrigatório")
        UUID materialId,

        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(
            value = "0.0",
            message = "Quantidade não pode ser negativa"
        )
        BigDecimal quantidadeKg
    ) {}

    public record PedidoCooperativaRequest(
        @NotNull
        UUID pedidoId,

        @NotNull
        UUID cooperativaId,

        @NotNull
        UUID statusId
    ) {}

    public record AlterarSenhaRequest(
        @NotBlank
        String senhaAtual,

        @NotBlank
        @Size(min = 6)
        String novaSenha
    ) {}

    public record EquipeRequest(
        @NotNull(message = "ID do gestor é obrigatório")
        UUID gestorId,

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 255)
        String nome,

        Boolean estaAtiva
    ) {}

    public record EquipeCooperadoRequest(
        @NotNull(message = "ID da equipe é obrigatório")
        UUID equipeId,

        @NotNull(message = "ID do cooperado é obrigatório")
        UUID cooperadoId
    ) {}

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class RateioGeralRequest {
        private UUID gestorId;
        private UUID cooperativaId;
        private LocalDateTime dataInicio;
        private LocalDateTime dataFim;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class RateioProporcionalsRequest {
        private UUID gestorId;
        private UUID cooperativaId;
        private LocalDateTime dataInicio;
        private LocalDateTime dataFim;
    }
}