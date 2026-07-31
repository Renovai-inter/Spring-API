package com.renovai.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.renovai.api.dto.response.Responses.ResultadoRateioIndividualResponse;

public class Responses {

    public record LoginResponse(String token, String tipo, String email, String role) {
        public LoginResponse(String token, String email, String role) {
            this(token, "Bearer", email, role);
        }
    }

    public record CooperativaResponse(
        UUID cooperativaId,
        String nome,
        String descricao,
        Integer numeroCooperados,
        String horarioFuncionamento
    ) {}

    public record EmpresaResponse(
        UUID empresaId,
        String nome,
        String descricao,
        UUID materialId,
        String materialCategoria
    ) {}

    public record MaterialResponse(
        UUID materialId,
        UUID categoriaId,
        String categoriaNome,
        BigDecimal precoSugerido,
        Boolean estaDisponivel
    ) {}

    public record UsuarioResponse(
        UUID usuarioId,
        String nome,
        String cpf,
        LocalDate dataNascimento
    ) {}

    public record EnderecoResponse(
        UUID enderecoId,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade
    ) {}

    public record PerfilResponse(
        UUID perfilId,
        String email,
        String cnpj,
        Boolean estaAtivo,
        LocalDateTime dataCriacao,
        UUID empresaId,
        String empresaNome,
        UUID cooperativaId,
        String cooperativaNome
    ) {}

    public record ColetaResponse(
        UUID coletaId,
        UUID cooperadoId,
        String cooperadoNome,
        String statusAtual,
        String origem,
        BigDecimal quantidadeKg,
        LocalDateTime dataColeta
    ) {}

    public record TriagemResponse(
        UUID triagemId,
        UUID equipeId,
        String equipeNome,
        UUID coletaId,
        UUID materialId,
        String materialCategoria,
        String statusAtual,
        BigDecimal quantidadeKg,
        BigDecimal quantidadeRejeitoKg,
        LocalDateTime dataTriagem
    ) {}

    public record PedidoResponse(
        UUID pedidoId,
        UUID empresaId,
        String empresaNome,
        LocalDateTime dataPedido,
        LocalDateTime dataConclusao
    ) {}

    public record ItemResponse(
        UUID itemId,
        UUID pedidoId,
        UUID materialId,
        String materialCategoria,
        BigDecimal quantidadeKg,
        BigDecimal precoUnitario
    ) {}

    public record PrimeiroAcessoResponse(
        boolean primeiroAcesso,
        String mensagem
    ) {}

    public record EstoqueResponse(
        UUID estoqueId,
        UUID cooperativaId,
        String cooperativaNome,
        UUID materialId,
        String materialCategoria,
        BigDecimal quantidadeKg,
        LocalDateTime dataAtualizacao
    ) {}

    public record AvaliacaoResponse(
        UUID avaliacaoId,
        UUID avaliadorId,
        UUID avaliadoId,
        UUID pedidoId,
        Integer nota,
        String comentario,
        LocalDateTime dataAvaliacao
    ) {}

    public record PedidoCooperativaResponse(
        UUID pedidoCooperativaId,
        UUID pedidoId,
        UUID cooperativaId,
        String cooperativaNome,
        String statusAtual
    ) {}

    public record EquipeResponse(
        UUID equipeId,
        UUID cooperativaId,
        String cooperativaNome,
        UUID gestorId,
        String gestorNome,
        String nome,
        Boolean estaAtiva,
        LocalDateTime dataCriacao
    ) {}

    public record EquipeCooperadoResponse(
        UUID equipeCooperadoId,
        UUID equipeId,
        String equipeNome,
        UUID cooperadoId,
        String cooperadoNome
    ) {}

    public record RateioResponse(
        UUID rateioId,
        UUID gestorId,
        String gestorNome,
        UUID cooperativaId,
        String cooperativaNome,
        UUID tipoRateioId,
        String tipoRateioNome,
        LocalDateTime dataRateio,
        Long quantidadePessoas,
        BigDecimal valorTotalDistribuido
    ) {}

    public record ResultadoRateioIndividualResponse(
        UUID funcionarioId,
        String funcionarioNome,
        String cargo,
        Boolean isGestor,
        BigDecimal valorRateio,
        Integer quantidadeColetas,
        Integer quantidadeTriagens,
        BigDecimal percentualParticipacao
    ) {}

    public record RateioRealizadoResponse(
        UUID rateioId,
        UUID gestorId,
        String gestorNome,
        UUID cooperativaId,
        String cooperativaNome,
        String tipoRateio,
        LocalDateTime dataRateio,
        BigDecimal valorTotalVendas,
        BigDecimal valorTotalDistribuido,
        Long quantidadePessoas,
        List<ResultadoRateioIndividualResponse> distribuicao
    ) {}

    public record RateioDetalheResponse(
        UUID rateioId,
        UUID gestorId,
        String gestorNome,
        UUID cooperativaId,
        String cooperativaNome,
        String tipoRateio,
        LocalDateTime dataRateio,
        BigDecimal valorTotalDistribuido,
        List<ResultadoRateioIndividualResponse> funcionarios
    ) {}

    public record RateioListaResponse(
        UUID rateioId,
        String gestorNome,
        String cooperativaNome,
        String tipoRateio,
        LocalDateTime dataRateio,
        Long quantidadePessoas,
        BigDecimal valorTotalDistribuido
    ) {}

    public record RateioFuncionarioResponse(
        UUID rateioFuncionarioId,
        UUID rateioId,
        LocalDateTime dataRateio,
        String tipoRateio,
        UUID funcionarioId,
        String funcionarioNome,
        BigDecimal valorRateio,
        String cooperativaNome
    ) {}

}