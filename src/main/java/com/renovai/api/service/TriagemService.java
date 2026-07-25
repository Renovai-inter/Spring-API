package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.TriagemRequest;
import com.renovai.api.dto.response.Responses.TriagemResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.*;
import com.renovai.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class TriagemService {

    private final TriagemRepository repository;
    private final EquipeRepository equipeRepository;
    private final ColetaRepository coletaRepository;
    private final MaterialRepository materialRepository;
    private final StatusRepository statusRepository;
    private final EstoqueRepository estoqueRepository;
    private final CooperativaRepository cooperativaRepository;

    public TriagemService(TriagemRepository repository,
                          EquipeRepository equipeRepository,
                          ColetaRepository coletaRepository,
                          MaterialRepository materialRepository,
                          StatusRepository statusRepository,
                          EstoqueRepository estoqueRepository,
                          CooperativaRepository cooperativaRepository) {
        this.repository = repository;
        this.equipeRepository = equipeRepository;
        this.coletaRepository = coletaRepository;
        this.materialRepository = materialRepository;
        this.statusRepository = statusRepository;
        this.estoqueRepository = estoqueRepository;
        this.cooperativaRepository = cooperativaRepository;
    }

    @Transactional(readOnly = true)
    public List<TriagemResponse> listarTodas() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TriagemResponse buscarPorId(Integer id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<TriagemResponse> listarPorColeta(Integer coletaId) {
        return repository.findByColeta_ColetaId(coletaId).stream().map(this::toResponse).toList();
    }

    public TriagemResponse criar(TriagemRequest request) {
        Equipe equipe = equipeRepository.findById(request.equipeId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipe", request.equipeId()));
        Coleta coleta = coletaRepository.findById(request.coletaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Coleta", request.coletaId()));
        Material material = materialRepository.findById(request.materialId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material", request.materialId()));

        Status status = null;
        if (request.statusId() != null) {
            status = statusRepository.findById(request.statusId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Status", request.statusId()));
        }

        Triagem triagem = Triagem.builder()
                .equipe(equipe)
                .coleta(coleta)
                .material(material)
                .status(status)
                .quantidadeKg(request.quantidadeKg())
                .quantidadeRejeitoKg(request.quantidadeRejeitoKg())
                .build();

        Triagem saved = repository.save(triagem);
        atualizarEstoque(equipe.getGestor().getCooperativa(), material, request.quantidadeKg());
        return toResponse(saved);
    }

    public TriagemResponse atualizar(Integer id, TriagemRequest request) {
        Triagem triagem = findOrThrow(id);
        triagem.setQuantidadeKg(request.quantidadeKg());
        triagem.setQuantidadeRejeitoKg(request.quantidadeRejeitoKg());
        if (request.statusId() != null) {
            Status status = statusRepository.findById(request.statusId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Status", request.statusId()));
            triagem.setStatus(status);
        }
        return toResponse(repository.save(triagem));
    }

    public void deletar(Integer id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    /** Incrementa estoque da cooperativa para o material triado */
    private void atualizarEstoque(Cooperativa cooperativa, Material material, BigDecimal quantidade) {
        Estoque estoque = estoqueRepository
                .findByCooperativa_CooperativaIdAndMaterial_MaterialId(
                        cooperativa.getCooperativaId(), material.getMaterialId())
                .orElseGet(() -> Estoque.builder()
                        .cooperativa(cooperativa)
                        .material(material)
                        .quantidadeKg(BigDecimal.ZERO)
                        .build());

        estoque.setQuantidadeKg(estoque.getQuantidadeKg().add(quantidade));
        estoqueRepository.save(estoque);
    }

    private Triagem findOrThrow(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Triagem", id));
    }

    private TriagemResponse toResponse(Triagem t) {
        return new TriagemResponse(
                t.getTriagemId(),
                t.getEquipe().getEquipeId(),
                t.getEquipe().getNome(),
                t.getColeta().getColetaId(),
                t.getMaterial().getMaterialId(),
                t.getMaterial().getCategoria(),
                t.getStatus() != null ? t.getStatus().getStatusAtual() : null,
                t.getQuantidadeKg(),
                t.getQuantidadeRejeitoKg(),
                t.getDataTriagem()
        );
    }
}
