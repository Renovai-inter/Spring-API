package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.MaterialRequest;
import com.renovai.api.dto.response.Responses.MaterialResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.Material;
import com.renovai.api.repository.MaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MaterialService {

    private final MaterialRepository repository;

    public MaterialService(MaterialRepository repository) {
        this.repository = repository;
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
    public MaterialResponse buscarPorId(Integer id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> buscarPorCategoria(String categoria) {
        return repository.findByCategoriaContainingIgnoreCase(categoria)
                .stream().map(this::toResponse).toList();
    }

    public MaterialResponse criar(MaterialRequest request) {
        Material material = Material.builder()
                .categoria(request.categoria())
                .precoSugerido(request.precoSugerido())
                .estaDisponivel(request.estaDisponivel() != null ? request.estaDisponivel() : true)
                .build();
        return toResponse(repository.save(material));
    }

    public MaterialResponse atualizar(Integer id, MaterialRequest request) {
        Material material = findOrThrow(id);
        material.setCategoria(request.categoria());
        material.setPrecoSugerido(request.precoSugerido());
        if (request.estaDisponivel() != null) material.setEstaDisponivel(request.estaDisponivel());
        return toResponse(repository.save(material));
    }

    public void deletar(Integer id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    private Material findOrThrow(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material", id));
    }

    public Material findEntityById(Integer id) {
        return findOrThrow(id);
    }

    private MaterialResponse toResponse(Material m) {
        return new MaterialResponse(m.getMaterialId(), m.getCategoria(), m.getPrecoSugerido(), m.getEstaDisponivel());
    }
}
