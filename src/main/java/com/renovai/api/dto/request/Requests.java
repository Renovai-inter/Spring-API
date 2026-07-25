package com.renovai.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

        Integer materialId
    ) {}

    public record PreCadastroRequest(
        String nome,
        String cpf,
        String senhaTemporaria,
        Integer cargoId,

        @NotNull(message = "ID da cooperativa é obrigatório")
        Integer cooperativaId

    ) {}

    public record MaterialRequest(
        @NotBlank(message = "Categoria é obrigatória")
        @Size(max = 100)
        String categoria,

        @DecimalMin(value = "0.0", message = "Preço sugerido deve ser positivo")
        BigDecimal precoSugerido,

        Boolean estaDisponivel
    ) {}

    public record UsuarioRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 255)
        String nome,

        @Pattern(regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}", message = "CPF inválido (use 000.000.000-00)")
        String cpf,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 6, message = "Senha deve ter no mínimo 6 caracteres")
        String senha,

        LocalDate dataNascimento
    ) {}

    public record EnderecoRequest(
        @Pattern(regexp = "\\d{5}-\\d{3}", message = "CEP inválido (use 00000-000)")
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
        Integer empresaId,
        Integer cooperativaId,
        Integer enderecoId,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        @Pattern(regexp = "\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}", message = "CNPJ inválido")
        String cnpj
    ) {}

    public record ColetaRequest(
        @NotNull(message = "ID do cooperado é obrigatório")
        Integer cooperadoId,

        Integer statusId,

        @Size(max = 255)
        String origem,

        @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero")
        BigDecimal quantidadeKg
    ) {}

    public record TriagemRequest(
        @NotNull(message = "ID da equipe é obrigatório")
        Integer equipeId,

        @NotNull(message = "ID da coleta é obrigatório")
        Integer coletaId,

        @NotNull(message = "ID do material é obrigatório")
        Integer materialId,

        Integer statusId,

        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero")
        BigDecimal quantidadeKg,

        @DecimalMin(value = "0.0", message = "Quantidade de rejeito não pode ser negativa")
        BigDecimal quantidadeRejeitoKg
    ) {}

    public record PedidoRequest(
        @NotNull(message = "ID da empresa é obrigatório")
        Integer empresaId
    ) {}

    public record ItemRequest(
        @NotNull(message = "ID do pedido é obrigatório")
        Integer pedidoId,

        @NotNull(message = "ID do material é obrigatório")
        Integer materialId,

        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero")
        BigDecimal quantidadeKg,

        @DecimalMin(value = "0.0", message = "Preço unitário não pode ser negativo")
        BigDecimal precoUnitario
    ) {}

    public record AvaliacaoRequest(
        @NotNull(message = "ID do avaliador é obrigatório")
        Integer avaliadorId,

        @NotNull(message = "ID do avaliado é obrigatório")
        Integer avaliadoId,

        Integer pedidoId,

        @Min(value = 1, message = "Nota mínima é 1")
        @Max(value = 5, message = "Nota máxima é 5")
        Integer nota,

        String comentario
    ) {}

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class RateioRequest {
        private Integer gestorId;
        private Integer tipoRateioId;
    }

    public record EstoqueRequest(
        @NotNull(message = "ID da cooperativa é obrigatório")
        Integer cooperativaId,

        @NotNull(message = "ID do material é obrigatório")
        Integer materialId,

        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(value = "0.0", message = "Quantidade não pode ser negativa")
        BigDecimal quantidadeKg
    ) {}

    public record PedidoCooperativaRequest(
        @NotNull Integer pedidoId,
        @NotNull Integer cooperativaId,
        @NotNull Integer statusId
    ) {}

    public record AlterarSenhaRequest(
        @NotBlank String senhaAtual,
        @NotBlank @Size(min = 6) String novaSenha
    ) {}

    public record EquipeRequest(
        @NotNull(message = "ID do gestor é obrigatório")
        Integer gestorId,

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 255)
        String nome,

        Boolean estaAtiva
    ) {}
    
    public record EquipeCooperadoRequest(
        @NotNull(message = "ID da equipe é obrigatório")
        Integer equipeId,

        @NotNull(message = "ID do cooperado é obrigatório")
        Integer cooperadoId
    ) {}

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class RateioGeralRequest {
        private Integer gestorId;
        private Integer cooperativaId;
        private LocalDateTime dataInicio;
        private LocalDateTime dataFim;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public class RateioProporcionalsRequest {
        private Integer gestorId;
        private Integer cooperativaId;
        private LocalDateTime dataInicio;
        private LocalDateTime dataFim;
    }
}
