package com.renovai.api.dto.response;

import com.renovai.api.dto.response.Responses.EstoqueResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class EmpresaContaResponses {
    public record CadastroResponse(String token, String email, String role, UUID empresaId) {}

    public record MeuPerfil(
            UUID perfilId,
            UUID empresaId,
            String nomeEmpresa,
            String descricao,
            String email,
            String cnpj,
            String endereco,
            String telefone,
            String cidade,
            UUID materialId,
            UUID categoriaId,
            String categoriaNome) {}

    public record Dashboard(
            long totalPedidosEnviados,
            long totalPedidosAceitos,
            long totalPedidosConcluidos,
            BigDecimal valorTotalNegociado) {}

    public record Interesse(UUID empresaId, UUID categoriaId, String categoriaNome) {}

    public record PerfilPublico(
            UUID cooperativaId,
            String nome,
            String descricao,
            String cidade,
            String endereco,
            String email,
            String telefone,
            String horarioFuncionamento,
            Double mediaAvaliacoes,
            long totalAvaliacoes,
            List<EstoqueResponse> materiaisDisponiveis) {}

    public record AvaliacaoPublica(
            UUID avaliacaoId,
            UUID avaliadorId,
            String avaliadorNome,
            UUID avaliadoId,
            UUID pedidoId,
            Integer nota,
            String comentario,
            String dataAvaliacao) {}

    public record Estrelas(
            long estrelas1, long estrelas2, long estrelas3, long estrelas4, long estrelas5) {}
}
