package com.renovai.api.service;
 
import com.renovai.api.dto.request.Requests.AtualizarQuantidadeEstoqueRequest;
import com.renovai.api.dto.request.Requests.EstoqueRequest;
import com.renovai.api.dto.response.Responses.EstoqueResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.*;
import com.renovai.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
 
@Service
@Transactional
public class EstoqueService {
 
    private final EstoqueRepository repository;
    private final CooperativaRepository cooperativaRepository;
    private final MaterialRepository materialRepository;
 
    public EstoqueService(EstoqueRepository repository,
                          CooperativaRepository cooperativaRepository,
                          MaterialRepository materialRepository) {
        this.repository = repository;
        this.cooperativaRepository = cooperativaRepository;
        this.materialRepository = materialRepository;
    }
 
    @Transactional(readOnly = true)
    public List<EstoqueResponse> listarTodos() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }
 
    @Transactional(readOnly = true)
    public EstoqueResponse buscarPorId(UUID id) {
        return toResponse(findOrThrow(id));
    }
 
    @Transactional(readOnly = true)
    public List<EstoqueResponse> listarPorCooperativa(UUID cooperativaId) {
        return repository.findByCooperativa_CooperativaId(cooperativaId)
                .stream().map(this::toResponse).toList();
    }
 
    @Transactional(readOnly = true)
    public List<EstoqueResponse> listarDisponiveisPorCooperativa(UUID cooperativaId) {
        return repository.findDisponiveisByCooperativa(cooperativaId)
                .stream().map(this::toResponse).toList();
    }
 
    public EstoqueResponse criar(EstoqueRequest request) {
        Cooperativa cooperativa = cooperativaRepository.findById(request.cooperativaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", request.cooperativaId()));
        Material material = materialRepository.findById(request.materialId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material", request.materialId()));
 
        Estoque estoque = Estoque.builder()
                .cooperativa(cooperativa)
                .material(material)
                .quantidadeKg(request.quantidadeKg())
                .build();
        return toResponse(repository.save(estoque));
    }
 
    public EstoqueResponse atualizar(UUID id, EstoqueRequest request) {
        Estoque estoque = findOrThrow(id);
        estoque.setQuantidadeKg(request.quantidadeKg());
        estoque.setDataAtualizacao(LocalDateTime.now());
        return toResponse(repository.save(estoque));
    }
 
    public EstoqueResponse atualizarQuantidade(UUID id, AtualizarQuantidadeEstoqueRequest request) {
        Estoque estoque = findOrThrow(id);
        estoque.setQuantidadeKg(request.novaQuantidadeKg());
        estoque.setDataAtualizacao(LocalDateTime.now());
        return toResponse(repository.save(estoque));
    }
 
    public void deletar(UUID id) {
        findOrThrow(id);
        repository.deleteById(id);
    }
 
    private Estoque findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Estoque", id));
    }
 
    private EstoqueResponse toResponse(Estoque e) {
        return new EstoqueResponse(
                e.getEstoqueId(),
                e.getCooperativa().getCooperativaId(),
                e.getCooperativa().getNome(),
                e.getMaterial().getMaterialId(),
                e.getMaterial().getCategoria() != null
                        ? e.getMaterial().getCategoria().getNomeCategoria() : null,
                e.getQuantidadeKg(),
                e.getDataAtualizacao()
        );
    }
}