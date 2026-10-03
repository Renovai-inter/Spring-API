package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.RateioGeralRequest;
import com.renovai.api.dto.request.Requests.RateioIndividualRequest;
import com.renovai.api.dto.request.Requests.RateioProporcionalsRequest;
import com.renovai.api.dto.response.Responses.*;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.*;
import com.renovai.api.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class RateioService {

    private final RateioRepository rateioRepository;
    private final RateioFuncionarioRepository rateioFuncionarioRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final CooperativaRepository cooperativaRepository;
    private final TipoRateioRepository tipoRateioRepository;
    private final ColetaRepository coletaRepository;
    private final TriagemRepository triagemRepository;
    private final ItemRepository itemRepository;
    private final PerfilRepository perfilRepository;

    @Transactional
    public RateioRealizadoResponse executarRateioIndividual(
            RateioIndividualRequest req, String email) {
        if (req.dataInicio().isAfter(req.dataFim())
                || !YearMonth.from(req.dataInicio()).equals(YearMonth.from(req.dataFim())))
            throw new RegraDeNegocioException("Informe um período válido dentro do mesmo mês.");
        if (req.participantes().isEmpty()
                || req.participantes().stream().map(p -> p.cooperadoId()).distinct().count()
                        != req.participantes().size())
            throw new RegraDeNegocioException("Informe participantes sem repetições.");
        BigDecimal soma = BigDecimal.ZERO;
        for (var participante : req.participantes()) {
            if (participante.percentual() == null
                    || participante.percentual().signum() <= 0
                    || participante.percentual().compareTo(new BigDecimal("100")) > 0)
                throw new RegraDeNegocioException("Percentual individual inválido.");
            soma = soma.add(participante.percentual());
        }
        if (soma.compareTo(new BigDecimal("100")) != 0)
            throw new RegraDeNegocioException("A soma dos percentuais deve ser 100%.");
        Funcionario gestor =
                funcionarioRepository
                        .findById(req.gestorId())
                        .orElseThrow(
                                () -> new RecursoNaoEncontradoException("Gestor", req.gestorId()));
        validarGestorDaCooperativa(gestor, req.cooperativaId());
        if (!"ATIVO".equals(gestor.getStatusFuncionario()))
            throw new RegraDeNegocioException("O gestor deve estar ativo.");
        boolean autorizado =
                gestor.getUsuario().getEmail() != null
                        && gestor.getUsuario().getEmail().equalsIgnoreCase(email);
        if (!autorizado)
            autorizado =
                    perfilRepository
                            .findByEmailIgnoreCase(email)
                            .filter(
                                    p ->
                                            Boolean.TRUE.equals(p.getEstaAtivo())
                                                    && p.getCooperativa() != null
                                                    && p.getCooperativa()
                                                            .getCooperativaId()
                                                            .equals(req.cooperativaId()))
                            .isPresent();
        if (!autorizado)
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "O gestor não corresponde à conta autenticada.");
        Cooperativa cooperativa =
                cooperativaRepository
                        .buscarComBloqueio(req.cooperativaId())
                        .orElseThrow(
                                () ->
                                        new RecursoNaoEncontradoException(
                                                "Cooperativa", req.cooperativaId()));
        LocalDate mes = req.dataInicio().toLocalDate().withDayOfMonth(1);
        if (rateioRepository.existsByCooperativa_CooperativaIdAndMesReferencia(
                req.cooperativaId(), mes))
            throw new RegraDeNegocioException(
                    "Já existe rateio para esta cooperativa no mês informado.");
        List<Funcionario> participantes = new ArrayList<>();
        for (var participante : req.participantes()) {
            Funcionario cooperado =
                    funcionarioRepository
                            .findById(participante.cooperadoId())
                            .orElseThrow(
                                    () ->
                                            new RecursoNaoEncontradoException(
                                                    "Funcionário", participante.cooperadoId()));
            if (!"ATIVO".equals(cooperado.getStatusFuncionario())
                    || !cooperado.getCooperativa().getCooperativaId().equals(req.cooperativaId()))
                throw new RegraDeNegocioException(
                        "Todos os participantes devem estar ativos e pertencer à cooperativa.");
            participantes.add(cooperado);
        }
        TipoRateio tipo =
                req.tipoRateioId() == null
                        ? tipoRateioRepository
                                .findByTipoRateio("PROPORCIONAL")
                                .orElseThrow(
                                        () ->
                                                new RegraDeNegocioException(
                                                        "Tipo PROPORCIONAL não cadastrado."))
                        : tipoRateioRepository
                                .findById(req.tipoRateioId())
                                .orElseThrow(
                                        () ->
                                                new RecursoNaoEncontradoException(
                                                        "Tipo de rateio", req.tipoRateioId()));
        BigDecimal total =
                calcularTotalVendasPeriodo(req.cooperativaId(), req.dataInicio(), req.dataFim())
                        .setScale(2, RoundingMode.HALF_UP);
        if (total.signum() <= 0)
            throw new RegraDeNegocioException(
                    "Não há vendas registradas no período informado para realizar o rateio.");
        List<BigDecimal> exatos =
                req.participantes().stream()
                        .map(p -> total.multiply(p.percentual()).divide(new BigDecimal("100")))
                        .toList();
        List<BigDecimal> valores =
                new ArrayList<>(
                        exatos.stream().map(v -> v.setScale(2, RoundingMode.DOWN)).toList());
        int centavos =
                total.subtract(valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add))
                        .movePointRight(2)
                        .intValueExact();
        List<Integer> ordem =
                IntStream.range(0, valores.size())
                        .boxed()
                        .sorted(
                                Comparator.<Integer, BigDecimal>comparing(
                                                i -> exatos.get(i).subtract(valores.get(i)))
                                        .reversed())
                        .toList();
        for (int i = 0; i < centavos; i++) {
            int indice = ordem.get(i);
            valores.set(indice, valores.get(indice).add(new BigDecimal("0.01")));
        }
        for (BigDecimal valor : valores)
            if (valor.precision() > 10)
                throw new RegraDeNegocioException("Valor individual excede o limite do banco.");
        Rateio rateio =
                rateioRepository.saveAndFlush(
                        Rateio.builder()
                                .gestor(gestor)
                                .cooperativa(cooperativa)
                                .tipoRateio(tipo)
                                .mesReferencia(mes)
                                .dataRateio(LocalDateTime.now())
                                .build());
        List<ResultadoRateioIndividualResponse> distribuicao = new ArrayList<>();
        for (int i = 0; i < participantes.size(); i++) {
            Funcionario cooperado = participantes.get(i);
            rateioFuncionarioRepository.save(
                    RateioFuncionario.builder()
                            .rateio(rateio)
                            .cooperado(cooperado)
                            .valorRateio(valores.get(i))
                            .build());
            distribuicao.add(
                    new ResultadoRateioIndividualResponse(
                            cooperado.getFuncionarioId(),
                            cooperado.getUsuario().getNome(),
                            cooperado.getCargo().getCargo(),
                            cooperado
                                    .getCargo()
                                    .getCargo()
                                    .toUpperCase(Locale.ROOT)
                                    .contains("GESTOR"),
                            valores.get(i),
                            0,
                            0,
                            req.participantes().get(i).percentual()));
        }
        rateioFuncionarioRepository.flush();
        return new RateioRealizadoResponse(
                rateio.getRateioId(),
                gestor.getFuncionarioId(),
                gestor.getUsuario().getNome(),
                cooperativa.getCooperativaId(),
                cooperativa.getNome(),
                tipo.getTipoRateio(),
                rateio.getDataRateio(),
                total,
                total,
                (long) participantes.size(),
                distribuicao);
    }

    @Transactional
    public RateioRealizadoResponse executarRateioGeral(RateioGeralRequest req) {
        Funcionario gestor = funcionarioRepository.findById(req.gestorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Gestor", req.gestorId()));

        Cooperativa cooperativa = cooperativaRepository.findById(req.cooperativaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", req.cooperativaId()));

        validarGestorDaCooperativa(gestor, req.cooperativaId());

        List<Funcionario> cooperados = funcionarioRepository.findAtivosByCooperativa(req.cooperativaId());
        if (cooperados.isEmpty()) {
            throw new RegraDeNegocioException("Nenhum cooperado ativo encontrado para a cooperativa " + req.cooperativaId());
        }

        BigDecimal totalVendas = calcularTotalVendasPeriodo(req.cooperativaId(), req.dataInicio(), req.dataFim());
        if (totalVendas.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("Não há vendas registradas no período informado para realizar o rateio.");
        }

        TipoRateio tipoRateio = tipoRateioRepository.findByTipoRateio("GERAL")
                .orElseGet(() -> tipoRateioRepository.save(
                        TipoRateio.builder().tipoRateio("GERAL")
                                .descricao("Divisão igualitária entre todos os cooperados ativos").build()));

        LocalDate mesReferencia = req.dataInicio().toLocalDate().withDayOfMonth(1);
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
        Funcionario gestor = funcionarioRepository.findById(req.gestorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Gestor", req.gestorId()));

        Cooperativa cooperativa = cooperativaRepository.findById(req.cooperativaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", req.cooperativaId()));

        validarGestorDaCooperativa(gestor, req.cooperativaId());

        List<Funcionario> cooperados = funcionarioRepository.findAtivosByCooperativa(req.cooperativaId());
        if (cooperados.isEmpty()) {
            throw new RegraDeNegocioException("Nenhum cooperado ativo encontrado para a cooperativa " + req.cooperativaId());
        }

        BigDecimal totalVendas = calcularTotalVendasPeriodo(req.cooperativaId(), req.dataInicio(), req.dataFim());
        if (totalVendas.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraDeNegocioException("Não há vendas registradas no período informado para realizar o rateio.");
        }

        record Pontuacao(Funcionario funcionario, long coletas, long triagens, long total) {}

        List<Pontuacao> pontuacoes = cooperados.stream().map(c -> {
            long coletas = coletaRepository.countColetasPorPeriodo(c.getFuncionarioId(), req.dataInicio(), req.dataFim());
            long triagens = triagemRepository.countTriagensPorPeriodo(c.getFuncionarioId(), req.dataInicio(), req.dataFim());
            long totalPontos = coletas + triagens;
            return new Pontuacao(c, coletas, triagens, totalPontos);
        }).toList();

        long totalPontos = pontuacoes.stream().mapToLong(Pontuacao::total).sum();
        
        if (totalPontos == 0) {
            throw new RegraDeNegocioException(
                "Nenhuma atividade de coleta ou triagem registrada no período para cálculo de rateio proporcional. " +
                "Registre coletas/triagens ou utilize rateio geral."
            );
        }

        TipoRateio tipoRateio = tipoRateioRepository.findByTipoRateio("PROPORCIONAL")
                .orElseGet(() -> tipoRateioRepository.save(
                        TipoRateio.builder().tipoRateio("PROPORCIONAL")
                                .descricao("Divisão proporcional à produtividade individual no período").build()));

        LocalDate mesReferencia = req.dataInicio().toLocalDate().withDayOfMonth(1);

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
