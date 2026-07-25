package com.renovai.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.renovai.api.dto.response.Responses.ResultadoRateioIndividualResponse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class Responses {

    public record LoginResponse(String token, String tipo, String email, String role) {
        public LoginResponse(String token, String email, String role) {
            this(token, "Bearer", email, role);
        }
    }

    public record CooperativaResponse(
        Integer cooperativaId,
        String nome,
        String descricao,
        Integer numeroCooperados,
        String horarioFuncionamento
    ) {}

    public record EmpresaResponse(
        Integer empresaId,
        String nome,
        String descricao,
        Integer materialId,
        String materialCategoria
    ) {}

    public record MaterialResponse(
        Integer materialId,
        String categoria,
        BigDecimal precoSugerido,
        Boolean estaDisponivel
    ) {}

    public record UsuarioResponse(
        Integer usuarioId,
        String nome,
        String cpf,
        LocalDate dataNascimento
    ) {}

    public record EnderecoResponse(
        Integer enderecoId,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade
    ) {}

    public record PerfilResponse(
        Integer perfilId,
        String email,
        String cnpj,
        Boolean estaAtivo,
        LocalDateTime dataCriacao,
        Integer empresaId,
        String empresaNome,
        Integer cooperativaId,
        String cooperativaNome
    ) {}

    public record ColetaResponse(
        Integer coletaId,
        Integer cooperadoId,
        String cooperadoNome,
        String statusAtual,
        String origem,
        BigDecimal quantidadeKg,
        LocalDateTime dataColeta
    ) {}

    public record TriagemResponse(
        Integer triagemId,
        Integer equipeId,
        String equipeNome,
        Integer coletaId,
        Integer materialId,
        String materialCategoria,
        String statusAtual,
        BigDecimal quantidadeKg,
        BigDecimal quantidadeRejeitoKg,
        LocalDateTime dataTriagem
    ) {}

    public record PedidoResponse(
        Integer pedidoId,
        Integer empresaId,
        String empresaNome,
        LocalDateTime dataPedido,
        LocalDateTime dataFinal
    ) {}
    

    public record ItemResponse(
        Integer itemId,
        Integer pedidoId,
        Integer materialId,
        String materialCategoria,
        BigDecimal quantidadeKg,
        BigDecimal precoUnitario
    ) {}

    public record PrimeiroAcessoResponse(
        boolean primeiroAcesso,
        String mensagem
    ) {}

    public record EstoqueResponse(
        Integer estoqueId,
        Integer cooperativaId,
        String cooperativaNome,
        Integer materialId,
        String materialCategoria,
        BigDecimal quantidadeKg,
        LocalDateTime dataAtualizacao
    ) {}

    public record AvaliacaoResponse(
        Integer avaliacaoId,
        Integer avaliadorId,
        Integer avaliadoId,
        Integer pedidoId,
        Integer nota,
        String comentario,
        LocalDateTime dataAvaliacao
    ) {}

    public record PedidoCooperativaResponse(
        Integer pedidoCooperativaId,
        Integer pedidoId,
        Integer cooperativaId,
        String cooperativaNome,
        String statusAtual
    ) {}

    public record EquipeResponse(
        Integer equipeId,
        Integer cooperativaId,
        String cooperativaNome,
        Integer gestorId,
        String gestorNome,
        String nome,
        Boolean estaAtiva,
        LocalDateTime dataCriacao
    ) {}

    public record EquipeCooperadoResponse(
        Integer equipeCooperadoId,
        Integer equipeId,
        String equipeNome,
        Integer cooperadoId,
        String cooperadoNome
    ) {}

    public record RateioResponse(
        Integer rateioId,
        Integer gestorId,
        String gestorNome,
        Integer cooperativaId,
        String cooperativaNome,
        Integer tipoRateioId,
        String tipoRateioNome,
        LocalDateTime dataRateio,
        Long quantidadePessoas,
        BigDecimal valorTotalDistribuido
    ) {}

    public record ResultadoRateioIndividualResponse(
        Integer funcionarioId,
        String funcionarioNome,
        String cargo,
        Boolean isGestor,
        BigDecimal valorRateio,
        Integer quantidadeColetas,
        Integer quantidadeTriagens,
        BigDecimal percentualParticipacao
    ) {}

    public record RateioRealizadoResponse(
        Integer rateioId,
        Integer gestorId,
        String gestorNome,
        Integer cooperativaId,
        String cooperativaNome,
        String tipoRateio,
        LocalDateTime dataRateio,
        BigDecimal valorTotalVendas,
        BigDecimal valorTotalDistribuido,
        Long quantidadePessoas,
        List<ResultadoRateioIndividualResponse> distribuicao
    ) {}

    public record RateioDetalheResponse(
        Integer rateioId,
        Integer gestorId,
        String gestorNome,
        Integer cooperativaId,
        String cooperativaNome,
        String tipoRateio,
        LocalDateTime dataRateio,
        BigDecimal valorTotalDistribuido,
        List<ResultadoRateioIndividualResponse> funcionarios
    ) {}

    public record RateioListaResponse(
        Integer rateioId,
        String gestorNome,
        String cooperativaNome,
        String tipoRateio,
        LocalDateTime dataRateio,
        Long quantidadePessoas,
        BigDecimal valorTotalDistribuido
    ) {}

    public record RateioFuncionarioResponse(
        Integer rateioFuncionarioId,
        Integer rateioId,
        LocalDateTime dataRateio,
        String tipoRateio,
        Integer funcionarioId,
        String funcionarioNome,
        BigDecimal valorRateio,
        String cooperativaNome
    ) {}

}
