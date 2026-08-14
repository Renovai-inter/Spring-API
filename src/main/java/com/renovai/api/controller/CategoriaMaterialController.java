 
package com.renovai.api.controller;
 
import com.renovai.api.dto.request.Requests.CategoriaMaterialRequest;
import com.renovai.api.dto.response.Responses.CategoriaMaterialArvoreResponse;
import com.renovai.api.dto.response.Responses.CategoriaMaterialResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.CategoriaMaterial;
import com.renovai.api.repository.CategoriaMaterialRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
import java.util.UUID;
 
@RestController
@RequestMapping("/categorias-material")
@Tag(name = "Categorias de Material", description = "Categorias de materiais recicláveis (Plástico, Papel, Metal, Vidro...)")
public class CategoriaMaterialController {
 
    private final CategoriaMaterialRepository repository;
 
    public CategoriaMaterialController(CategoriaMaterialRepository repository) {
        this.repository = repository;
    }
 
    @GetMapping
    @Operation(summary = "Listar todas as categorias")
    public ResponseEntity<List<CategoriaMaterialResponse>> listar() {
        return ResponseEntity.ok(repository.findAll().stream().map(this::toResponse).toList());
    }
 
    @GetMapping("/raiz")
    @Operation(summary = "Listar categorias raiz (sem pai)")
    public ResponseEntity<List<CategoriaMaterialResponse>> listarRaiz() {
        return ResponseEntity.ok(repository.findByCategoriaPaiIsNull().stream().map(this::toResponse).toList());
    }
 
    @GetMapping("/arvore")
    @Operation(summary = "Listar categorias em árvore (raiz + subcategorias)")
    public ResponseEntity<List<CategoriaMaterialArvoreResponse>> listarArvore() {
        return ResponseEntity.ok(
                repository.findByCategoriaPaiIsNull().stream()
                        .map(this::toArvoreResponse).toList()
        );
    }
 
    @GetMapping("/{id}/subcategorias")
    @Operation(summary = "Listar subcategorias de uma categoria")
    public ResponseEntity<List<CategoriaMaterialResponse>> listarSubcategorias(@PathVariable UUID id) {
        return ResponseEntity.ok(
                repository.findByCategoriaPai_CategoriaId(id).stream().map(this::toResponse).toList()
        );
    }
 
    @GetMapping("/{id}")
    @Operation(summary = "Buscar categoria por ID")
    public ResponseEntity<CategoriaMaterialResponse> buscarPorId(@PathVariable UUID id) {
        CategoriaMaterial c = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("CategoriaMaterial", id));
        return ResponseEntity.ok(toResponse(c));
    }
 
    @PostMapping
    @Operation(summary = "Criar categoria de material")
    public ResponseEntity<CategoriaMaterialResponse> criar(
            @RequestBody @Valid CategoriaMaterialRequest request) {
        CategoriaMaterial pai = null;
        if (request.categoriaPaiId() != null) {
            pai = repository.findById(request.categoriaPaiId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("CategoriaMaterial pai", request.categoriaPaiId()));
            if (repository.existsByNomeCategoriaAndCategoriaPai_CategoriaId(
                    request.nomeCategoria(), request.categoriaPaiId())) {
                throw new RegraDeNegocioException("Já existe subcategoria com esse nome nessa categoria pai.");
            }
        }
        CategoriaMaterial categoria = new CategoriaMaterial();
        categoria.setNomeCategoria(request.nomeCategoria());
        categoria.setCategoriaPai(pai);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(repository.save(categoria)));
    }
 
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar categoria")
    public ResponseEntity<CategoriaMaterialResponse> atualizar(
            @PathVariable UUID id,
            @RequestBody @Valid CategoriaMaterialRequest request) {
        CategoriaMaterial c = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("CategoriaMaterial", id));
        c.setNomeCategoria(request.nomeCategoria());
        if (request.categoriaPaiId() != null) {
            CategoriaMaterial pai = repository.findById(request.categoriaPaiId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("CategoriaMaterial pai", request.categoriaPaiId()));
            c.setCategoriaPai(pai);
        }
        return ResponseEntity.ok(toResponse(repository.save(c)));
    }
 
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar categoria")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("CategoriaMaterial", id));
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
 
    private CategoriaMaterialResponse toResponse(CategoriaMaterial c) {
        return new CategoriaMaterialResponse(
                c.getCategoriaId(),
                c.getCategoriaPai() != null ? c.getCategoriaPai().getCategoriaId() : null,
                c.getCategoriaPai() != null ? c.getCategoriaPai().getNomeCategoria() : null,
                c.getNomeCategoria()
        );
    }
 
    private CategoriaMaterialArvoreResponse toArvoreResponse(CategoriaMaterial c) {
        List<CategoriaMaterialArvoreResponse> subs = repository
                .findByCategoriaPai_CategoriaId(c.getCategoriaId()).stream()
                .map(this::toArvoreResponse).toList();
        return new CategoriaMaterialArvoreResponse(c.getCategoriaId(), c.getNomeCategoria(), subs);
    }
}