package com.renovai.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
        String horarioFuncionamento,
        String imagemUrl,
        String contatoPreferencial,
        UUID enderecoId
    ) {}

    public record EmpresaRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 255)
        String nome,

        String descricao,

        UUID materialId,

        String imagemUrl
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

        LocalDate dataNascimento,

        String imagemUrl
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
        BigDecimal quantidadeKg,
        String imagemUrl,
        String tipoColeta,
        UUID rotaId

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
        BigDecimal quantidadeRejeitoKg,
        String imagemUrl
    ) {}

    public record PedidoRequest(
        @NotNull(message = "ID da empresa é obrigatório")
        UUID empresaId,
        String observacao
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

    public record RotaRequest(
        @NotNull UUID cooperativaId,
        @NotBlank @Size(max = 255) String nome,
        Boolean estaAtiva
    ) {}
    
    public record RotaEnderecoRequest(
        @NotNull UUID rotaId,
        @NotNull UUID enderecoId,
        @NotBlank @Size(max = 255) String nomeLocal,
        String tipoLocal,
        @NotNull @Min(1) Integer ordem
    ) {}

    public record NegociacaoRequest(
        @NotNull UUID pedidoId,
        @NotNull UUID cooperativaId,
        @NotNull UUID empresaId,
        @NotNull UUID statusId,
        BigDecimal valorTotal
    ) {}
    
    public record NegociacaoItemRequest(
        @NotNull UUID negociacaoId,
        @NotNull UUID materialId,
        @NotNull @DecimalMin("0.001") BigDecimal quantidadeKg,
        @NotNull @DecimalMin("0.0") BigDecimal precoUnitario
    ) {}
    
    public record NegociacaoMensagemRequest(
        @NotNull UUID negociacaoId,
        @NotNull UUID remetenteId,
        @NotBlank String mensagem,
        String tipoMensagem
    ) {}
    
    public record ContrapropostaRequest(
        @NotNull UUID negociacaoId,
        BigDecimal valorTotal,
        List<NegociacaoItemRequest> itens,
        String observacao
    ) {}
    
    public record RecusarNegociacaoRequest(
        @NotBlank String justificativa
    ) {}
    
    public record FecharNegociacaoRequest(
        @NotNull @DecimalMin("0.0") BigDecimal valorFinal,
        String observacao
    ) {}

    public record EmpresaMaterialInteresseRequest(
        @NotNull UUID empresaId,
        @NotNull UUID categoriaId
    ) {}

    public record FavoritoRequest(
        @NotNull UUID empresaId,
        @NotNull UUID cooperativaId
    ) {}

    public record DespesaRequest(
        @NotNull UUID cooperativaId,
        @NotBlank @Size(max = 255) String nome,
        @NotBlank String tipoDespesa,  // "FIXA" | "VARIAVEL"
        Boolean estaAtiva
    ) {}
    
    public record LancamentoDespesaRequest(
        @NotNull UUID despesaId,
        @NotNull @DecimalMin("0.0") BigDecimal valor,
        @NotNull LocalDate mesReferencia
    ) {}

    public record CategoriaMaterialRequest(
        UUID categoriaPaiId,
        @NotBlank @Size(max = 100) String nomeCategoria
    ) {}
    public record AtualizarStatusColetaRequest(
        @NotNull UUID statusId
    ) {}

    public record AtualizarStatusTriagemRequest(
        @NotNull UUID statusId
    ) {}

    public record ConcluirTriagemRequest(
        @NotNull BigDecimal quantidadeFinalKg,
        String observacao
    ) {}

    public record ConcluirPedidoRequest(
        @NotNull @DecimalMin("0.0") BigDecimal valorFinal,
        String observacao
    ) {}

    public record EsqueciSenhaRequest(
        @NotBlank @Email String email
    ) {}

    public record RedefinirSenhaRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 6) String novaSenha
    ) {}

    public record AtualizarQuantidadeEstoqueRequest(
        @NotNull @DecimalMin("0.0") BigDecimal novaQuantidadeKg,
        String motivo
    ) {}
    public record CalcularRejeitoRequest(
        @NotNull(message = "Quantidade da triagem é obrigatória")
        @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero")
        BigDecimal triagemQuantidadeKg,

        @NotNull(message = "ID da coleta é obrigatório")
        UUID coletaId
    ) {}

    public record CalcularRateioRequest(
        @NotNull(message = "ID da cooperativa é obrigatório")
        UUID cooperativaId,

        @NotNull(message = "Mês de referência é obrigatório")
        LocalDateTime mesReferencia
    ) {}

    public record CalcularTotalRequest(
        @NotNull(message = "ID da cooperativa é obrigatório")
        UUID cooperativaId,

        @NotNull(message = "Data inicial é obrigatória")
        LocalDateTime dataInicio,

        @NotNull(message = "Data final é obrigatória")
        LocalDateTime dataFim
    ) {}

    public record CalcularTotalCategoriaRequest(
        @NotNull(message = "ID da categoria é obrigatório")
        UUID categoriaId,

        LocalDateTime dataInicio,

        LocalDateTime dataFim
    ) {}

    public record AceitarPedidoCooperativaRequest(
        @NotNull(message = "ID do pedido da cooperativa é obrigatório")
        UUID pedidoCooperativaId,

        @NotNull(message = "ID do status aceito é obrigatório")
        UUID statusAceitoId
    ) {}

    public record FecharNegociacaoProcedureRequest(
        @NotNull(message = "ID da negociação é obrigatório")
        UUID negociacaoId,

        @NotNull(message = "É obrigatório informar se a negociação foi aceita")
        Boolean aceito
    ) {}

    public record FecharRateioMensalRequest(
        @NotNull(message = "ID da cooperativa é obrigatório")
        UUID cooperativaId,

        @NotNull(message = "ID do gestor é obrigatório")
        UUID gestorId,

        @NotNull(message = "Mês de referência é obrigatório")
        LocalDateTime mesReferencia
    ) {}

    public record RegistrarMovimentacaoTriagemRequest(
        @NotNull(message = "ID da equipe é obrigatório")
        UUID equipeId,

        @NotNull(message = "ID da coleta é obrigatório")
        UUID coletaId,

        @NotNull(message = "ID do material é obrigatório")
        UUID materialId,

        @NotNull(message = "ID do status é obrigatório")
        UUID statusId,

        @NotNull(message = "Quantidade é obrigatória")
        @DecimalMin(value = "0.001", message = "Quantidade deve ser maior que zero")
        BigDecimal quantidadeKg,

        @DecimalMin(
            value = "0.0",
            message = "Quantidade de rejeito não pode ser negativa"
        )
        BigDecimal quantidadeRejeitoKg,

        LocalDateTime dataTriagem
    ) {}
}