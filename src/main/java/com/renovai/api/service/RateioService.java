package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.RateioGeralRequest;
import com.renovai.api.dto.request.Requests.RateioProporcionalsRequest;
import com.renovai.api.dto.response.Responses.*;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.*;
import com.renovai.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RateioService {

    private final RateioRepository rateioRepository;
    private final RateioFuncionarioRepository rateioFuncionarioRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final CooperativaRepository cooperativaRepository;
    private final TipoRateioRepository tipoRateioRepository;
    private final ColetaRepository coletaRepository;
    private final ItemRepository itemRepository;



    @Transactional
    public RateioRealizadoResponse executarRateioGeral(RateioGeralRequest req) {
        Funcionario gestor = funcionarioRepository.findById(req.getGestorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Gestor", req.getGestorId()));

        Cooperativa cooperativa = cooperativaRepository.findById(req.getCooperativaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", req.getCooperativaId()));

        validarGestorDaCooperativa(gestor, req.getCooperativaId());

        List<Funcionario> cooperados = funcionarioRepository.findAtivosByCooperativa(req.getCooperativaId());
        if (cooperados.isEmpty()) {
            throw new RegraDeNegocioException("Nenhum cooperado ativo encontrado para a cooperativa " + req.getCooperativaId());
        }

        BigDecimal totalVendas = calcularTotalVendasPeriodo(req.getCooperativaId(), req.getDataInicio(), req.getDataFim());
        if (totalVendas.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("Não há vendas registradas no período informado para realizar o rateio.");
        }

        TipoRateio tipoRateio = tipoRateioRepository.findByTipoRateio("GERAL")
                .orElseGet(() -> tipoRateioRepository.save(
                        TipoRateio.builder().tipoRateio("GERAL")
                                .descricao("Divisão igualitária entre todos os cooperados ativos").build()));

        // mesReferencia obrigatório: primeiro dia do mês de início
        LocalDate mesReferencia = req.getDataInicio().toLocalDate().withDayOfMonth(1);


        Rateio rateio = rateioRepository.save(Rateio.builder()
                .gestor(gestor)
                .cooperativa(cooperativa)
                .tipoRateio(tipoRateio)
                .mesReferencia(mesReferencia)
                .dataRateio(LocalDateTime.now())
                .build());

        BigDecimal valorPorPessoa = totalVendas.divide(BigDecimal.valueOf(cooperados.size()), 2, RoundingMode.DOWN);

        List<ResultadoRateioIndividualResponse> distribuicao = cooperados.stream().map(c -> {
            rateioFuncionarioRepository.save(RateioFuncionario.builder()
                    .rateio(rateio).cooperado(c).valorRateio(valorPorPessoa).build());
            return new ResultadoRateioIndividualResponse(
                    c.getFuncionarioId(), c.getUsuario().getNome(), c.getCargo().getCargo(),
                    c.getCargo().getCargo().contains("GESTOR"),
                    valorPorPessoa, 0, 0, calcularPercentual(valorPorPessoa, totalVendas));
        }).toList();

        return new RateioRealizadoResponse(
                rateio.getRateioId(), gestor.getFuncionarioId(), gestor.getUsuario().getNome(),
                gestor.getCooperativa().getCooperativaId(), gestor.getCooperativa().getNome(),
                "GERAL", rateio.getDataRateio(), totalVendas,
                valorPorPessoa.multiply(BigDecimal.valueOf(cooperados.size())),
                (long) cooperados.size(), distribuicao);
    }

    @Transactional
    public RateioRealizadoResponse executarRateioProporcional(RateioProporcionalsRequest req) {
        Funcionario gestor = funcionarioRepository.findById(req.getGestorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Gestor", req.getGestorId()));

        Cooperativa cooperativa = cooperativaRepository.findById(req.getCooperativaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", req.getCooperativaId()));

        validarGestorDaCooperativa(gestor, req.getCooperativaId());

        List<Funcionario> cooperados = funcionarioRepository.findAtivosByCooperativa(req.getCooperativaId());
        if (cooperados.isEmpty()) {
            throw new RegraDeNegocioException("Nenhum cooperado ativo encontrado para a cooperativa " + req.getCooperativaId());
        }

        BigDecimal totalVendas = calcularTotalVendasPeriodo(req.getCooperativaId(), req.getDataInicio(), req.getDataFim());
        if (totalVendas.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("Não há vendas registradas no período informado para realizar o rateio.");
        }

        record Pontuacao(Funcionario funcionario, long coletas, long triagens, long total) {}

        List<Pontuacao> pontuacoes = cooperados.stream().map(c -> {
            long coletas = coletaRepository.countColetasPorPeriodo(c.getFuncionarioId(), req.getDataInicio(), req.getDataFim());
            return new Pontuacao(c, coletas, 0L, coletas);
        }).toList();

        long totalPontos = pontuacoes.stream().mapToLong(Pontuacao::total).sum();
        if (totalPontos == 0) {
            pontuacoes = pontuacoes.stream().map(p -> new Pontuacao(p.funcionario(), 0L, 0L, 1L)).toList();
            totalPontos = cooperados.size();
        }

        TipoRateio tipoRateio = tipoRateioRepository.findByTipoRateio("PROPORCIONAL")
                .orElseGet(() -> tipoRateioRepository.save(
                        TipoRateio.builder().tipoRateio("PROPORCIONAL")
                                .descricao("Divisão proporcional à produtividade individual no período").build()));

        LocalDate mesReferencia = req.getDataInicio().toLocalDate().withDayOfMonth(1);

        Rateio rateio = rateioRepository.save(Rateio.builder()
                .gestor(gestor)
                .cooperativa(cooperativa)
                .tipoRateio(tipoRateio)
                .mesReferencia(mesReferencia)
                .dataRateio(LocalDateTime.now())
                .build());

        final long totalPontosFinal = totalPontos;
        List<ResultadoRateioIndividualResponse> distribuicao = pontuacoes.stream().map(p -> {
            BigDecimal percentual = BigDecimal.valueOf(p.total())
                    .divide(BigDecimal.valueOf(totalPontosFinal), 6, RoundingMode.HALF_UP);
            BigDecimal valor = totalVendas.multiply(percentual).setScale(2, RoundingMode.DOWN);
            rateioFuncionarioRepository.save(RateioFuncionario.builder()
                    .rateio(rateio).cooperado(p.funcionario()).valorRateio(valor).build());
            return new ResultadoRateioIndividualResponse(
                    p.funcionario().getFuncionarioId(), p.funcionario().getUsuario().getNome(),
                    p.funcionario().getCargo().getCargo(),
                    p.funcionario().getCargo().getCargo().contains("GESTOR"),
                    valor, (int) p.coletas(), (int) p.triagens(),
                    percentual.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
        }).toList();

        BigDecimal totalDistribuido = rateioFuncionarioRepository.sumValoresByRateio(rateio.getRateioId());

        return new RateioRealizadoResponse(
                rateio.getRateioId(), gestor.getFuncionarioId(), gestor.getUsuario().getNome(),
                gestor.getCooperativa().getCooperativaId(), gestor.getCooperativa().getNome(),
                "PROPORCIONAL", rateio.getDataRateio(), totalVendas, totalDistribuido,
                (long) cooperados.size(), distribuicao);
    }

    public List<RateioListaResponse> listarTodos(UUID cooperativaId) {
        List<Rateio> rateios = cooperativaId != null
                ? rateioRepository.findByCooperativa(cooperativaId)
                : rateioRepository.findAll();
        return rateios.stream().map(this::toListaResponse).toList();
    }

    public RateioDetalheResponse buscarPorId(UUID rateioId) {
        Rateio rateio = rateioRepository.findById(rateioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Rateio", rateioId));

        List<RateioFuncionario> distribuicao = rateioFuncionarioRepository.findByRateio(rateioId);
        BigDecimal totalDistribuido = rateioFuncionarioRepository.sumValoresByRateio(rateioId);

        List<ResultadoRateioIndividualResponse> funcionarios = distribuicao.stream().map(rf ->
                new ResultadoRateioIndividualResponse(
                        rf.getCooperado().getFuncionarioId(), rf.getCooperado().getUsuario().getNome(),
                        rf.getCooperado().getCargo().getCargo(),
                        rf.getCooperado().getCargo().getCargo().contains("GESTOR"),
                        rf.getValorRateio(), 0, 0,
                        calcularPercentual(rf.getValorRateio(), totalDistribuido))).toList();

        return new RateioDetalheResponse(
                rateio.getRateioId(), rateio.getGestor().getFuncionarioId(),
                rateio.getGestor().getUsuario().getNome(),
                rateio.getGestor().getCooperativa().getCooperativaId(),
                rateio.getGestor().getCooperativa().getNome(),
                rateio.getTipoRateio() != null ? rateio.getTipoRateio().getTipoRateio() : "N/A",
                rateio.getDataRateio(), totalDistribuido, funcionarios);
    }

    public List<RateioListaResponse> listarPorCooperativa(UUID cooperativaId) {
        return rateioRepository.findByCooperativa(cooperativaId).stream().map(this::toListaResponse).toList();
    }

    public List<RateioFuncionarioResponse> listarDistribuicaoPorRateio(UUID rateioId) {
        rateioRepository.findById(rateioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Rateio", rateioId));
        return rateioFuncionarioRepository.findByRateio(rateioId).stream().map(rf ->
                new RateioFuncionarioResponse(
                        rf.getRateioFuncionarioId(), rf.getRateio().getRateioId(),
                        rf.getRateio().getDataRateio(),
                        rf.getRateio().getTipoRateio() != null ? rf.getRateio().getTipoRateio().getTipoRateio() : "N/A",
                        rf.getCooperado().getFuncionarioId(), rf.getCooperado().getUsuario().getNome(),
                        rf.getValorRateio(), rf.getCooperado().getCooperativa().getNome())).toList();
    }

    private void validarGestorDaCooperativa(Funcionario gestor, UUID cooperativaId) {
        if (!gestor.getCooperativa().getCooperativaId().equals(cooperativaId)) {
            throw new RegraDeNegocioException("O gestor informado não pertence à cooperativa " + cooperativaId);
        }
        String cargo = gestor.getCargo().getCargo().toUpperCase();
        if (!cargo.contains("GESTOR") && !cargo.contains("ADMIN")) {
            throw new RegraDeNegocioException("Somente gestores ou administradores podem executar rateios.");
        }
    }

    private BigDecimal calcularTotalVendasPeriodo(UUID cooperativaId, LocalDateTime inicio, LocalDateTime fim) {
        return itemRepository.sumValoresPorCooperativaEPeriodo(cooperativaId, inicio, fim);
    }

    private BigDecimal calcularPercentual(BigDecimal valor, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return valor.divide(total, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    private RateioListaResponse toListaResponse(Rateio r) {
        long qtd = rateioFuncionarioRepository.countByRateio_RateioId(r.getRateioId());
        BigDecimal total = rateioFuncionarioRepository.sumValoresByRateio(r.getRateioId());
        return new RateioListaResponse(
                r.getRateioId(), r.getGestor().getUsuario().getNome(),
                r.getGestor().getCooperativa().getNome(),
                r.getTipoRateio() != null ? r.getTipoRateio().getTipoRateio() : "N/A",
                r.getDataRateio(), qtd, total != null ? total : BigDecimal.ZERO);
    }
}