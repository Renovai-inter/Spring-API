package com.renovai.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    public record RateioResponse(
        Integer rateioId,
        Integer gestorId,
        String gestorNome,
        Integer tipoRateioId,
        String tipoRateio,
        LocalDateTime dataRateio
    ) {}

    public record PedidoCooperativaResponse(
        Integer pedidoCooperativaId,
        Integer pedidoId,
        Integer cooperativaId,
        String cooperativaNome,
        String statusAtual
    ) {}
}
