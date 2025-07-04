package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.CooperativaRequest;
import com.renovai.api.dto.response.Responses.CooperativaResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.Cooperativa;
import com.renovai.api.repository.CooperativaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CooperativaService {

    private final CooperativaRepository repository;

    public CooperativaService(CooperativaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CooperativaResponse> listarTodas() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CooperativaResponse buscarPorId(Integer id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<CooperativaResponse> buscarPorNome(String nome) {
        return repository.findByNomeContainingIgnoreCase(nome).stream()
                .map(this::toResponse).toList();
    }

    public CooperativaResponse criar(CooperativaRequest request) {
        Cooperativa cooperativa = Cooperativa.builder()
                .nome(request.nome())
                .descricao(request.descricao())
                .numeroCooperados(request.numeroCooperados())
                .horarioFuncionamento(request.horarioFuncionamento())
                .build();
        return toResponse(repository.save(cooperativa));
    }

    public CooperativaResponse atualizar(Integer id, CooperativaRequest request) {
        Cooperativa cooperativa = findOrThrow(id);
        cooperativa.setNome(request.nome());
        cooperativa.setDescricao(request.descricao());
        cooperativa.setNumeroCooperados(request.numeroCooperados());
        cooperativa.setHorarioFuncionamento(request.horarioFuncionamento());
        return toResponse(repository.save(cooperativa));
    }

    public void deletar(Integer id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    private Cooperativa findOrThrow(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", id));
    }

    private CooperativaResponse toResponse(Cooperativa c) {
        return new CooperativaResponse(
                c.getCooperativaId(),
                c.getNome(),
                c.getDescricao(),
                c.getNumeroCooperados(),
                c.getHorarioFuncionamento()
        );
    }
}
