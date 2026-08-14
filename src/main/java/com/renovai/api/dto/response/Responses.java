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
        String horarioFuncionamento,
        String imagemUrl,
        String contatoPreferencial,
        String cidade 
    ) {}

    public record EmpresaResponse(
        UUID empresaId,
        String nome,
        String descricao,
        UUID materialId,
        String materialCategoria,
        String imagemUrl
    ) {}

    public record MaterialResponse(
        UUID materialId,
        UUID categoriaId,
        String categoriaNome,
        BigDecimal precoSugerido,
        Boolean estaDisponivel,
        UUID cooperativaId,
        String imagemUrl 
    ) {}

    public record UsuarioResponse(
        UUID usuarioId,
        String nome,
        String cpf,
        LocalDate dataNascimento,
        String imagemUrl,
        LocalDateTime ultimoAcesso
    ) {}

    public record EnderecoResponse(
        UUID enderecoId,
        String cep,
        String logradouro,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String tipo
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
        LocalDateTime dataColeta,
        String tipoColeta,
        String imagemUrl,
        UUID rotaId 
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
        LocalDateTime dataTriagem,
        String imagemUrl,
        List<String> cooperadosNomes
    ) {}

    public record PedidoResponse(
        UUID pedidoId,
        UUID empresaId,
        String empresaNome,
        LocalDateTime dataPedido,
        LocalDateTime dataConclusao,
        String statusAtual,
        BigDecimal valorTotal,
        String observacao  
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

    public record DistribuicaoEstrelas(
        long estrelas0,
        long estrelas1,
        long estrelas2,
        long estrelas3,
        long estrelas4,
        long estrelas5
    ) {}

    public record RotaResponse(
        UUID rotaId,
        UUID cooperativaId,
        String cooperativaNome,
        String nome,
        Boolean estaAtiva,
        List<RotaEnderecoResponse> enderecos
    ) {}
    
    public record RotaEnderecoResponse(
        UUID rotaEnderecoId,
        UUID rotaId,
        UUID enderecoId,
        String nomeLocal,
        String tipoLocal,
        Integer ordem,
        String logradouro,
        String numero,
        String bairro,
        String cidade
    ) {}

    public record NegociacaoResponse(
        UUID negociacaoId,
        UUID pedidoId,
        UUID cooperativaId,
        String cooperativaNome,
        UUID empresaId,
        String empresaNome,
        String statusAtual,
        BigDecimal valorTotal,
        LocalDateTime dataInicio,
        LocalDateTime dataFechamento,
        List<NegociacaoItemResponse> itens
    ) {}
    
    public record NegociacaoItemResponse(
        UUID negociacaoItemId,
        UUID negociacaoId,
        UUID materialId,
        String materialCategoria,
        BigDecimal quantidadeKg,
        BigDecimal precoUnitario
    ) {}
    
    public record NegociacaoMensagemResponse(
        UUID mensagemId,
        UUID negociacaoId,
        UUID remetenteId,
        String remetenteNome,
        String mensagem,
        String tipoMensagem,
        LocalDateTime dataEnvio
    ) {}
    public record EmpresaMaterialInteresseResponse(
        UUID empresaMaterialId,
        UUID empresaId,
        UUID categoriaId,
        String categoriaNome
    ) {}

    public record FavoritoResponse(
        UUID favoritoId,
        UUID empresaId,
        UUID cooperativaId,
        String cooperativaNome,
        String cooperativaImagem,
        LocalDateTime dataCriacao
    ) {}

    public record EmpresaDashboardResponse(
        Long totalPedidosEnviados,
        Long totalPedidosAceitos,
        BigDecimal valorTotalNegociado,
        Long totalCooperativasFavoritadas
    ) {}

    public record CooperativaPerfilPublicoResponse(
        UUID cooperativaId,
        String nome,
        String descricao,
        String imagemUrl,
        String contatoPreferencial,
        String horarioFuncionamento,
        String cidade,
        Double mediaAvaliacoes,
        Long totalAvaliacoes,
        List<EstoqueResponse> materiaisDisponiveis
    ) {}

    public record DespesaResponse(
        UUID despesaId,
        UUID cooperativaId,
        String cooperativaNome,
        String nome,
        String tipoDespesa,
        Boolean estaAtiva
    ) {}
    
    public record LancamentoDespesaResponse(
        UUID lancamentoId,
        UUID despesaId,
        String despesaNome,
        String tipoDespesa,
        BigDecimal valor,
        LocalDate mesReferencia,
        LocalDateTime dataLancamento
    ) {}
    
    public record TotalDespesasMesResponse(
        LocalDate mesReferencia,
        BigDecimal totalFixas,
        BigDecimal totalVariaveis,
        BigDecimal totalGeral
    ) {}

    public record CategoriaMaterialResponse(
        UUID categoriaId,
        UUID categoriaPaiId,
        String categoriaPaiNome,
        String nomeCategoria
    ) {}
    
    public record CategoriaMaterialArvoreResponse(
        UUID categoriaId,
        String nomeCategoria,
        List<CategoriaMaterialArvoreResponse> subcategorias
    ) {}

    public record MovimentacaoEstoqueResponse(
        UUID movimentacaoId,
        UUID estoqueId,
        UUID cooperativaId,
        String cooperativaNome,
        UUID materialId,
        String materialCategoria,
        UUID triagemId,
        UUID itemId,
        BigDecimal quantidadeKg,
        String tipoMovimentacao,
        LocalDateTime dataMovimentacao
    ) {}

    public record MediaAvaliacoesResponse(
        BigDecimal mediaAvaliacoes
    ) {}

    public record NumeroCooperadosResponse(
        Integer numeroCooperados
    ) {}

    public record QuantidadeRejeitoResponse(
        BigDecimal quantidadeRejeitoKg
    ) {}

    public record RateioAutomaticoResponse(
        BigDecimal valorIndividual
    ) {}

    public record TotalAcumuladoResponse(
        BigDecimal totalAcumulado
    ) {}

    public record TotalLiquidoResponse(
        BigDecimal totalLiquido
    ) {}

    public record TotalKgCategoriaResponse(
        BigDecimal totalKg
    ) {}

    public record ProcedureResponse(
        boolean sucesso,
        String mensagem
    ) {}

}