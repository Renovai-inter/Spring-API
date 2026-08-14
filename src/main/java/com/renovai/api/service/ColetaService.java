package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.AtualizarStatusColetaRequest;
import com.renovai.api.dto.request.Requests.ColetaRequest;
import com.renovai.api.dto.response.Responses.ColetaResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.Coleta;
import com.renovai.api.model.Funcionario;
import com.renovai.api.model.Rota;
import com.renovai.api.model.Status;
import com.renovai.api.repository.ColetaRepository;
import com.renovai.api.repository.FuncionarioRepository;
import com.renovai.api.repository.RotaRepository;
import com.renovai.api.repository.StatusRepository;
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

    public ColetaService(ColetaRepository repository,
                         FuncionarioRepository funcionarioRepository,
                         StatusRepository statusRepository,
                         RotaRepository rotaRepository) {
        this.repository = repository;
        this.funcionarioRepository = funcionarioRepository;
        this.statusRepository = statusRepository;
        this.rotaRepository = rotaRepository;
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
        Funcionario cooperado = funcionarioRepository.findById(request.cooperadoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário", request.cooperadoId()));

        Status status = null;
        if (request.statusId() != null) {
            status = statusRepository.findById(request.statusId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Status", request.statusId()));
        }

        Rota rota = null;
        if (request.rotaId() != null) {
            rota = rotaRepository.findById(request.rotaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Rota", request.rotaId()));
        }

        Coleta coleta = new Coleta();
        coleta.setCooperado(cooperado);
        coleta.setStatus(status);
        coleta.setOrigem(request.origem());
        coleta.setQuantidadeKg(request.quantidadeKg());
        coleta.setTipoColeta(request.tipoColeta());
        coleta.setImagemUrl(request.imagemUrl());
        coleta.setRota(rota);

        return toResponse(repository.save(coleta));
    }

    public ColetaResponse atualizar(UUID id, ColetaRequest request) {
        Coleta coleta = findOrThrow(id);
        coleta.setOrigem(request.origem());
        coleta.setQuantidadeKg(request.quantidadeKg());
        coleta.setTipoColeta(request.tipoColeta());
        coleta.setImagemUrl(request.imagemUrl());

        if (request.statusId() != null) {
            Status status = statusRepository.findById(request.statusId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Status", request.statusId()));
            coleta.setStatus(status);
        }

        if (request.rotaId() != null) {
            Rota rota = rotaRepository.findById(request.rotaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Rota", request.rotaId()));
            coleta.setRota(rota);
        }

        return toResponse(repository.save(coleta));
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
        return new ColetaResponse(
                c.getEventoId(),
                c.getCooperado().getFuncionarioId(),
                c.getCooperado().getUsuario() != null ? c.getCooperado().getUsuario().getNome() : null,
                c.getStatus() != null ? c.getStatus().getStatusAtual() : null,
                c.getOrigem(),
                c.getQuantidadeKg(),
                c.getDataEvento(),
                c.getTipoColeta(),
                c.getImagemUrl(),
                c.getRota() != null ? c.getRota().getRotaId() : null
        );
    }
}