package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.ColetaRequest;
import com.renovai.api.dto.response.Responses.ColetaResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.Coleta;
import com.renovai.api.model.Funcionario;
import com.renovai.api.model.Status;
import com.renovai.api.repository.ColetaRepository;
import com.renovai.api.repository.FuncionarioRepository;
import com.renovai.api.repository.StatusRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ColetaService {

    private final ColetaRepository repository;
    private final FuncionarioRepository funcionarioRepository;
    private final StatusRepository statusRepository;

    public ColetaService(ColetaRepository repository,
                         FuncionarioRepository funcionarioRepository,
                         StatusRepository statusRepository) {
        this.repository = repository;
        this.funcionarioRepository = funcionarioRepository;
        this.statusRepository = statusRepository;
    }

    @Transactional(readOnly = true)
    public List<ColetaResponse> listarTodas() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ColetaResponse buscarPorId(Integer id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ColetaResponse> listarPorCooperado(Integer cooperadoId) {
        return repository.findByCooperado_FuncionarioId(cooperadoId)
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

        Coleta coleta = Coleta.builder()
                .cooperado(cooperado)
                .status(status)
                .origem(request.origem())
                .quantidadeKg(request.quantidadeKg())
                .build();
        return toResponse(repository.save(coleta));
    }

    public ColetaResponse atualizar(Integer id, ColetaRequest request) {
        Coleta coleta = findOrThrow(id);
        coleta.setOrigem(request.origem());
        coleta.setQuantidadeKg(request.quantidadeKg());
        if (request.statusId() != null) {
            Status status = statusRepository.findById(request.statusId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Status", request.statusId()));
            coleta.setStatus(status);
        }
        return toResponse(repository.save(coleta));
    }

    public void deletar(Integer id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    private Coleta findOrThrow(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Coleta", id));
    }

    private ColetaResponse toResponse(Coleta c) {
        return new ColetaResponse(
                c.getColetaId(),
                c.getCooperado().getFuncionarioId(),
                c.getCooperado().getUsuario() != null ? c.getCooperado().getUsuario().getNome() : null,
                c.getStatus() != null ? c.getStatus().getStatusAtual() : null,
                c.getOrigem(),
                c.getQuantidadeKg(),
                c.getDataColeta()
        );
    }
}
