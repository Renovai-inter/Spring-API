package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.EmpresaRequest;
import com.renovai.api.dto.response.Responses.EmpresaResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.Empresa;
import com.renovai.api.model.Material;
import com.renovai.api.repository.EmpresaRepository;
import com.renovai.api.repository.MaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EmpresaService {

    private final EmpresaRepository repository;
    private final MaterialRepository materialRepository;

    public EmpresaService(EmpresaRepository repository, MaterialRepository materialRepository) {
        this.repository = repository;
        this.materialRepository = materialRepository;
    }

    @Transactional(readOnly = true)
    public List<EmpresaResponse> listarTodas() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EmpresaResponse buscarPorId(Integer id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<EmpresaResponse> buscarPorNome(String nome) {
        return repository.findByNomeContainingIgnoreCase(nome).stream().map(this::toResponse).toList();
    }

    public EmpresaResponse criar(EmpresaRequest request) {
        Material material = null;
        if (request.materialId() != null) {
            material = materialRepository.findById(request.materialId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Material", request.materialId()));
        }
        Empresa empresa = Empresa.builder()
                .nome(request.nome())
                .descricao(request.descricao())
                .material(material)
                .build();
        return toResponse(repository.save(empresa));
    }

    public EmpresaResponse atualizar(Integer id, EmpresaRequest request) {
        Empresa empresa = findOrThrow(id);
        empresa.setNome(request.nome());
        empresa.setDescricao(request.descricao());
        if (request.materialId() != null) {
            Material material = materialRepository.findById(request.materialId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Material", request.materialId()));
            empresa.setMaterial(material);
        }
        return toResponse(repository.save(empresa));
    }

    public void deletar(Integer id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    private Empresa findOrThrow(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa", id));
    }

    public Empresa findEntityById(Integer id) {
        return findOrThrow(id);
    }

    private EmpresaResponse toResponse(Empresa e) {
        return new EmpresaResponse(
                e.getEmpresaId(), e.getNome(), e.getDescricao(),
                e.getMaterial() != null ? e.getMaterial().getMaterialId() : null,
                e.getMaterial() != null ? e.getMaterial().getCategoria() : null
        );
    }
}
