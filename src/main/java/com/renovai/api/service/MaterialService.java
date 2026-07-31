package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.MaterialRequest;
import com.renovai.api.dto.response.Responses.MaterialResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.CategoriaMaterial;
import com.renovai.api.model.Cooperativa;
import com.renovai.api.model.Material;
import com.renovai.api.repository.CategoriaMaterialRepository;
import com.renovai.api.repository.CooperativaRepository;
import com.renovai.api.repository.MaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MaterialService {

    private final MaterialRepository repository;
    private final CategoriaMaterialRepository categoriaMaterialRepository;
    private final CooperativaRepository cooperativaRepository;

    public MaterialService(MaterialRepository repository,
                           CategoriaMaterialRepository categoriaMaterialRepository,
                           CooperativaRepository cooperativaRepository) {
        this.repository = repository;
        this.categoriaMaterialRepository = categoriaMaterialRepository;
        this.cooperativaRepository = cooperativaRepository;
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> listarTodos() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> listarDisponiveis() {
        return repository.findByEstaDisponivelTrue().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MaterialResponse buscarPorId(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> buscarPorCategoria(UUID categoriaId) {
        return repository.findByCategoria_CategoriaId(categoriaId)
                .stream().map(this::toResponse).toList();
    }

    public MaterialResponse criar(MaterialRequest request) {
        CategoriaMaterial categoria = categoriaMaterialRepository.findById(request.categoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("CategoriaMaterial", request.categoriaId()));

        Cooperativa cooperativa = null;
        if (request.cooperativaId() != null) {
            cooperativa = cooperativaRepository.findById(request.cooperativaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", request.cooperativaId()));
        }

        Material material = Material.builder()
                .categoria(categoria)
                .cooperativa(cooperativa)
                .precoSugerido(request.precoSugerido())
                .estaDisponivel(request.estaDisponivel() != null ? request.estaDisponivel() : true)
                .build();
        return toResponse(repository.save(material));
    }

    public MaterialResponse atualizar(UUID id, MaterialRequest request) {
        Material material = findOrThrow(id);
        CategoriaMaterial categoria = categoriaMaterialRepository.findById(request.categoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("CategoriaMaterial", request.categoriaId()));
        material.setCategoria(categoria);
        material.setPrecoSugerido(request.precoSugerido());
        if (request.estaDisponivel() != null) material.setEstaDisponivel(request.estaDisponivel());
        return toResponse(repository.save(material));
    }

    public void deletar(UUID id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    private Material findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material", id));
    }

    public Material findEntityById(UUID id) {
        return findOrThrow(id);
    }

    private MaterialResponse toResponse(Material m) {
        return new MaterialResponse(
                m.getMaterialId(),
                m.getCategoria() != null ? m.getCategoria().getCategoriaId() : null,
                m.getCategoria() != null ? m.getCategoria().getNomeCategoria() : null,
                m.getPrecoSugerido(),
                m.getEstaDisponivel()
        );
    }
}