package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.EquipeCooperadoRequest;
import com.renovai.api.dto.response.Responses.EquipeCooperadoResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.Equipe;
import com.renovai.api.model.EquipeCooperado;
import com.renovai.api.model.Funcionario;
import com.renovai.api.repository.EquipeCooperadoRepository;
import com.renovai.api.repository.EquipeRepository;
import com.renovai.api.repository.FuncionarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EquipeCooperadoService {

    private final EquipeCooperadoRepository repository;
    private final EquipeRepository equipeRepository;
    private final FuncionarioRepository funcionarioRepository;

    public EquipeCooperadoService(EquipeCooperadoRepository repository,
                                   EquipeRepository equipeRepository,
                                   FuncionarioRepository funcionarioRepository) {
        this.repository = repository;
        this.equipeRepository = equipeRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    @Transactional(readOnly = true)
    public List<EquipeCooperadoResponse> listarTodos() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EquipeCooperadoResponse> listarPorEquipe(Integer equipeId) {
        return repository.findByEquipe_EquipeId(equipeId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EquipeCooperadoResponse> listarPorCooperado(Integer cooperadoId) {
        return repository.findByCooperado_FuncionarioId(cooperadoId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EquipeCooperadoResponse buscarPorId(Integer id) {
        return toResponse(findOrThrow(id));
    }

    public EquipeCooperadoResponse adicionar(EquipeCooperadoRequest request) {
        Equipe equipe = equipeRepository.findById(request.equipeId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipe", request.equipeId()));

        Funcionario cooperado = funcionarioRepository.findById(request.cooperadoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionario", request.cooperadoId()));

        if (repository.existsByEquipe_EquipeIdAndCooperado_FuncionarioId(request.equipeId(), request.cooperadoId())) {
            throw new RegraDeNegocioException("Cooperado já pertence a esta equipe.");
        }

        if (!equipe.getGestor().getCooperativa().getCooperativaId()
                .equals(cooperado.getCooperativa().getCooperativaId())) {
            throw new RegraDeNegocioException("Cooperado deve pertencer à mesma cooperativa da equipe.");
        }

        EquipeCooperado equipeCooperado = EquipeCooperado.builder()
                .equipe(equipe)
                .cooperado(cooperado)
                .build();

        return toResponse(repository.save(equipeCooperado));
    }

    public void remover(Integer id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    public void removerPorEquipeECooperado(Integer equipeId, Integer cooperadoId) {
        if (!repository.existsByEquipe_EquipeIdAndCooperado_FuncionarioId(equipeId, cooperadoId)) {
            throw new RegraDeNegocioException("Vínculo entre equipe e cooperado não encontrado.");
        }
        repository.deleteByEquipe_EquipeIdAndCooperado_FuncionarioId(equipeId, cooperadoId);
    }

    private EquipeCooperado findOrThrow(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("EquipeCooperado", id));
    }

    private EquipeCooperadoResponse toResponse(EquipeCooperado ec) {
        return new EquipeCooperadoResponse(
                ec.getEquipeCooperadoId(),
                ec.getEquipe().getEquipeId(),
                ec.getEquipe().getNome(),
                ec.getCooperado().getFuncionarioId(),
                ec.getCooperado().getUsuario().getNome()
        );
    }
}