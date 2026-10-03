package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.AtualizarStatusColetaRequest;
import com.renovai.api.dto.request.Requests.ColetaRequest;
import com.renovai.api.dto.request.Requests.TriagemRequest;
import com.renovai.api.dto.response.Responses.ColetaResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.Coleta;
import com.renovai.api.model.Funcionario;
import com.renovai.api.model.Rota;
import com.renovai.api.model.Status;
import com.renovai.api.repository.ColetaRepository;
import com.renovai.api.repository.FuncionarioRepository;
import com.renovai.api.repository.RotaRepository;
import com.renovai.api.repository.StatusRepository;
import com.renovai.api.repository.TriagemRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ColetaService {

    private final ColetaRepository repository;
    private final FuncionarioRepository funcionarioRepository;
    private final StatusRepository statusRepository;
    private final RotaRepository rotaRepository;
    private final TriagemRepository triagemRepository;
    private final TriagemService triagemService;
    private final TriagemEstoqueService estoque;

    public ColetaService(ColetaRepository repository,
                         FuncionarioRepository funcionarioRepository,
                         StatusRepository statusRepository,
                         RotaRepository rotaRepository,
                         TriagemRepository triagemRepository,
                         TriagemService triagemService,
                         TriagemEstoqueService estoque) {
        this.repository = repository;
        this.funcionarioRepository = funcionarioRepository;
        this.statusRepository = statusRepository;
        this.rotaRepository = rotaRepository;
        this.triagemRepository = triagemRepository;
        this.triagemService = triagemService;
        this.estoque = estoque;
    }

    @Transactional(readOnly = true)
    public List<ColetaResponse> listarTodas() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ColetaResponse buscarPorId(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ColetaResponse> listarPorCooperado(UUID cooperadoId) {
        return repository.findByCooperado_FuncionarioId(cooperadoId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ColetaResponse> listarPorCooperativa(UUID cooperativaId) {
        return repository.findByCooperativa(cooperativaId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ColetaResponse> listarPorCooperativaETipo(UUID cooperativaId, String tipoColeta) {
        return repository.findByCooperativaAndTipo(cooperativaId, tipoColeta)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ColetaResponse> listarPorCooperativaEStatus(UUID cooperativaId, String status) {
        return repository.findByCooperativaAndStatus(cooperativaId, status)
                .stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ColetaResponse> listarPorRota(UUID rotaId) {
        return repository.findByRota_RotaId(rotaId)
                .stream().map(this::toResponse).toList();
    }

    public ColetaResponse criar(ColetaRequest request) {
        Funcionario cooperado =
                funcionarioRepository
                        .findById(request.cooperadoId())
                        .orElseThrow(
                                () ->
                                        new RecursoNaoEncontradoException(
                                                "Funcionário", request.cooperadoId()));

        Status status = null;
        if (request.statusId() != null) {
            status =
                    statusRepository
                            .findById(request.statusId())
                            .orElseThrow(
                                    () ->
                                            new RecursoNaoEncontradoException(
                                                    "Status", request.statusId()));
        }

        Rota rota = null;
        if (request.rotaId() != null) {
            rota = rotaRepository.findById(request.rotaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Rota", request.rotaId()));
        }

        Coleta coleta = new Coleta();
        coleta.setCooperado(cooperado);
        coleta.setStatus(status);
        coleta.setQuantidadeKg(request.quantidadeKg());
        coleta.setTipoColeta(request.tipoColeta());
        coleta.setImagemUrl(request.imagemUrl());
        coleta.setRota(rota);

        Coleta saved = repository.saveAndFlush(coleta);
        salvarMateriais(saved, request);
        return toResponse(saved);
    }

    public ColetaResponse atualizar(UUID id, ColetaRequest request) {
        estoque.bloquearColeta(id);
        Coleta coleta = findOrThrow(id);
        if (coleta.getQuantidadeKg().compareTo(request.quantidadeKg()) != 0
                && (request.materiais() == null || request.materiais().isEmpty())
                && triagemRepository.findByColeta_EventoId(id).stream().anyMatch(estoque::concluida))
            throw new RegraDeNegocioException("Informe os pesos dos materiais para corrigir uma coleta com entrada de estoque.");
        coleta.setQuantidadeKg(request.quantidadeKg());
        coleta.setTipoColeta(request.tipoColeta());
        coleta.setImagemUrl(request.imagemUrl());

        if (request.statusId() != null) {
            Status status =
                    statusRepository
                            .findById(request.statusId())
                            .orElseThrow(
                                    () ->
                                            new RecursoNaoEncontradoException(
                                                    "Status", request.statusId()));
            coleta.setStatus(status);
        }

        if (request.rotaId() != null) {
            Rota rota = rotaRepository.findById(request.rotaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Rota", request.rotaId()));
            coleta.setRota(rota);
        }

        Coleta saved = repository.saveAndFlush(coleta);
        salvarMateriais(saved, request);
        return toResponse(saved);
    }

    public ColetaResponse atualizarStatus(UUID id, AtualizarStatusColetaRequest request) {
        Coleta coleta = findOrThrow(id);
        Status status = statusRepository.findById(request.statusId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Status", request.statusId()));
        coleta.setStatus(status);
        return toResponse(repository.save(coleta));
    }

    public void deletar(UUID id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    private Coleta findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Coleta", id));
    }

    private ColetaResponse toResponse(Coleta c) {
        var materiais = triagemService.listarPorColeta(c.getEventoId());
        boolean precisaTriagem = materiais.isEmpty() || materiais.stream().anyMatch(t -> t.statusAtual() == null
                || !java.util.Set.of("CONCLUIDO", "CONCLUIDA", "FINALIZADO")
                .contains(NegociacaoFluxoService.normalizar(t.statusAtual())));
        return new ColetaResponse(
                c.getEventoId(),
                c.getCooperado().getFuncionarioId(),
                c.getCooperado().getUsuario() != null
                        ? c.getCooperado().getUsuario().getNome()
                        : null,
                c.getStatus() != null ? c.getStatus().getStatusAtual() : null,
                c.getQuantidadeKg(),
                c.getDataEvento(),
                c.getTipoColeta(),
                c.getImagemUrl(),
                c.getRota() != null ? c.getRota().getRotaId() : null,
                precisaTriagem,
                materiais);
    }

    private void salvarMateriais(Coleta coleta, ColetaRequest request) {
        if (request.materiais() == null || request.materiais().isEmpty()) {
            if (Boolean.FALSE.equals(request.precisaTriagem()))
                throw new RegraDeNegocioException(
                        "Informe os materiais separados para registrar a entrada de estoque.");
            return;
        }
        if (request.equipeId() == null)
            throw new RegraDeNegocioException(
                    "Informe a equipe responsável pelos materiais da coleta.");
        if (request.materiais().stream().map(m -> m.materialId()).distinct().count()
                != request.materiais().size())
            throw new RegraDeNegocioException("Informe materiais sem repetições.");
        var total =
                request.materiais().stream()
                        .map(m -> m.quantidadeKg())
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        if (total.compareTo(coleta.getQuantidadeKg()) != 0)
            throw new RegraDeNegocioException(
                    "A soma dos pesos dos materiais deve corresponder ao peso da coleta.");
        var existentes = triagemRepository.findByColeta_EventoId(coleta.getEventoId());
        if (existentes.stream()
                .anyMatch(
                        t ->
                                t.getQuantidadeRejeitoKg() != null
                                        && t.getQuantidadeRejeitoKg().signum() > 0))
            throw new RegraDeNegocioException(
                    "Altere materiais com rejeito pelos endpoints de triagem.");
        if (!existentes.isEmpty()
                && (existentes.size() != request.materiais().size()
                        || existentes.stream()
                                .anyMatch(
                                        t ->
                                                request.materiais().stream()
                                                        .noneMatch(
                                                                m ->
                                                                        m.materialId()
                                                                                .equals(
                                                                                        t.getMaterial()
                                                                                                .getMaterialId())))))
            throw new RegraDeNegocioException(
                    "Altere a separação dos materiais pelos endpoints de triagem.");
        boolean precisaTriagem =
                request.precisaTriagem() != null
                        ? request.precisaTriagem()
                        : existentes.isEmpty()
                                || existentes.stream().anyMatch(t -> !estoque.concluida(t));
        Status status =
                statusRepository
                        .findByReferenciaAndStatusAtual(
                                "TRIAGEM", precisaTriagem ? "Em triagem" : "Concluído")
                        .orElseThrow(
                                () ->
                                        new RegraDeNegocioException(
                                                "Status de triagem não cadastrado."));
        for (var material : request.materiais()) {
            var existente =
                    existentes.stream()
                            .filter(
                                    t ->
                                            t.getMaterial()
                                                    .getMaterialId()
                                                    .equals(material.materialId()))
                            .findFirst();
            TriagemRequest triagem =
                    new TriagemRequest(
                            request.equipeId(),
                            coleta.getEventoId(),
                            material.materialId(),
                            status.getStatusId(),
                            material.quantidadeKg(),
                            java.math.BigDecimal.ZERO,
                            request.imagemUrl());
            if (existente.isPresent()) {
                triagemService.atualizar(existente.get().getEventoId(), triagem);
            } else {
                triagemService.criar(triagem);
            }
        }
    }
}
