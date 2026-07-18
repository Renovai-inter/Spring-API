package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.EquipeRequest;
import com.renovai.api.dto.response.Responses.EquipeResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.Cooperativa;
import com.renovai.api.model.Equipe;
import com.renovai.api.model.Funcionario;
import com.renovai.api.repository.CooperativaRepository;
import com.renovai.api.repository.EquipeRepository;
import com.renovai.api.repository.FuncionarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EquipeService {

    private final EquipeRepository repository;
    private final CooperativaRepository cooperativaRepository;
    private final FuncionarioRepository funcionarioRepository;

    public EquipeService(EquipeRepository repository,
                         CooperativaRepository cooperativaRepository,
                         FuncionarioRepository funcionarioRepository) {
        this.repository = repository;
        this.cooperativaRepository = cooperativaRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    @Transactional(readOnly = true)
    public List<EquipeResponse> listarTodas() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EquipeResponse> listarAtivas() {
        return repository.findByEstaAtivaTrue().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<EquipeResponse> listarPorCooperativa(Integer cooperativaId) {
        return repository.findByCooperativa_CooperativaId(cooperativaId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EquipeResponse buscarPorId(Integer id) {
        return toResponse(findOrThrow(id));
    }

    public EquipeResponse criar(EquipeRequest request) {
        Cooperativa cooperativa = cooperativaRepository.findById(request.cooperativaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", request.cooperativaId()));

        Funcionario gestor = funcionarioRepository.findById(request.gestorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionario", request.gestorId()));

        Equipe equipe = Equipe.builder()
                .cooperativa(cooperativa)
                .gestor(gestor)
                .nome(request.nome())
                .estaAtiva(request.estaAtiva() != null ? request.estaAtiva() : true)
                .build();

        return toResponse(repository.save(equipe));
    }

    public EquipeResponse atualizar(Integer id, EquipeRequest request) {
        Equipe equipe = findOrThrow(id);

        Cooperativa cooperativa = cooperativaRepository.findById(request.cooperativaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", request.cooperativaId()));

        Funcionario gestor = funcionarioRepository.findById(request.gestorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionario", request.gestorId()));

        equipe.setCooperativa(cooperativa);
        equipe.setGestor(gestor);
        equipe.setNome(request.nome());
        if (request.estaAtiva() != null) equipe.setEstaAtiva(request.estaAtiva());

        return toResponse(repository.save(equipe));
    }

    public void deletar(Integer id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    private Equipe findOrThrow(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipe", id));
    }

    private EquipeResponse toResponse(Equipe e) {
        return new EquipeResponse(
                e.getEquipeId(),
                e.getCooperativa().getCooperativaId(),
                e.getCooperativa().getNome(),
                e.getGestor().getFuncionarioId(),
                e.getGestor().getUsuario().getNome(),
                e.getNome(),
                e.getEstaAtiva(),
                e.getDataCriacao()
        );
    }
}