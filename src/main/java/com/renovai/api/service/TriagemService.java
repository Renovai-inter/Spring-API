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
import java.util.UUID;

@Service
@Transactional
public class TriagemService {

    private final TriagemRepository repository;
    private final EquipeRepository equipeRepository;
    private final ColetaRepository coletaRepository;
    private final MaterialRepository materialRepository;
    private final StatusRepository statusRepository;
    private final EstoqueRepository estoqueRepository;

    public TriagemService(TriagemRepository repository,
                          EquipeRepository equipeRepository,
                          ColetaRepository coletaRepository,
                          MaterialRepository materialRepository,
                          StatusRepository statusRepository,
                          EstoqueRepository estoqueRepository) {
        this.repository = repository;
        this.equipeRepository = equipeRepository;
        this.coletaRepository = coletaRepository;
        this.materialRepository = materialRepository;
        this.statusRepository = statusRepository;
        this.estoqueRepository = estoqueRepository;
    }

    @Transactional(readOnly = true)
    public List<TriagemResponse> listarTodas() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TriagemResponse buscarPorId(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<TriagemResponse> listarPorColeta(UUID coletaId) {
        // PK de Coleta é eventoId — o repository usa findByColeta_EventoId
        return repository.findByColeta_EventoId(coletaId).stream().map(this::toResponse).toList();
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

        // Triagem não tem @Builder (herda EventoOperacional) — usa setters
        Triagem triagem = new Triagem();
        triagem.setEquipe(equipe);
        triagem.setColeta(coleta);
        triagem.setMaterial(material);
        triagem.setStatus(status);
        triagem.setQuantidadeKg(request.quantidadeKg());
        triagem.setQuantidadeRejeitoKg(
                request.quantidadeRejeitoKg() != null ? request.quantidadeRejeitoKg() : BigDecimal.ZERO);

        Triagem saved = repository.save(triagem);
        atualizarEstoque(equipe.getGestor().getCooperativa(), material, request.quantidadeKg());
        return toResponse(saved);
    }

    public TriagemResponse atualizar(UUID id, TriagemRequest request) {
        Triagem triagem = findOrThrow(id);
        triagem.setQuantidadeKg(request.quantidadeKg());
        triagem.setQuantidadeRejeitoKg(
                request.quantidadeRejeitoKg() != null ? request.quantidadeRejeitoKg() : BigDecimal.ZERO);
        if (request.statusId() != null) {
            Status status = statusRepository.findById(request.statusId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Status", request.statusId()));
            triagem.setStatus(status);
        }
        return toResponse(repository.save(triagem));
    }

    public void deletar(UUID id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    private void atualizarEstoque(Cooperativa cooperativa, Material material, BigDecimal quantidade) {
        Estoque estoque = estoqueRepository
                .findByCooperativa_CooperativaIdAndMaterial_MaterialId(
                        cooperativa.getCooperativaId(), material.getMaterialId())
                .orElseGet(() -> {
                    Estoque novo = new Estoque();
                    novo.setCooperativa(cooperativa);
                    novo.setMaterial(material);
                    novo.setQuantidadeKg(BigDecimal.ZERO);
                    return novo;
                });
        estoque.setQuantidadeKg(estoque.getQuantidadeKg().add(quantidade));
        estoqueRepository.save(estoque);
    }

    private Triagem findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Triagem", id));
    }

    private TriagemResponse toResponse(Triagem t) {
        return new TriagemResponse(
                t.getEventoId(),                // PK herdada de EventoOperacional
                t.getEquipe().getEquipeId(),
                t.getEquipe().getNome(),
                t.getColeta().getEventoId(),    // PK herdada da Coleta
                t.getMaterial().getMaterialId(),
                t.getMaterial().getCategoria() != null
                        ? t.getMaterial().getCategoria().getNomeCategoria() : null,
                t.getStatus() != null ? t.getStatus().getStatusAtual() : null,
                t.getQuantidadeKg(),
                t.getQuantidadeRejeitoKg(),
                t.getDataEvento()               // campo herdado de EventoOperacional
        );
    }
}