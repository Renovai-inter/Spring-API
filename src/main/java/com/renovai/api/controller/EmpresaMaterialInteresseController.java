package com.renovai.api.controller;
 
import com.renovai.api.dto.request.Requests.EmpresaMaterialInteresseRequest;
import com.renovai.api.dto.response.Responses.EmpresaMaterialInteresseResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.CategoriaMaterial;
import com.renovai.api.model.Empresa;
import com.renovai.api.model.EmpresaMaterialInteresse;
import com.renovai.api.repository.CategoriaMaterialRepository;
import com.renovai.api.repository.EmpresaMaterialInteresseRepository;
import com.renovai.api.repository.EmpresaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
import java.util.UUID;
 
@RestController
@RequestMapping("/empresas-materiais-interesse")
@Tag(name = "Materiais de Interesse", description = "Categorias de materiais que a empresa deseja comprar — tela 5.6")
public class EmpresaMaterialInteresseController {
 
    private final EmpresaMaterialInteresseRepository repository;
    private final EmpresaRepository empresaRepository;
    private final CategoriaMaterialRepository categoriaRepository;
 
    public EmpresaMaterialInteresseController(EmpresaMaterialInteresseRepository repository,
                                               EmpresaRepository empresaRepository,
                                               CategoriaMaterialRepository categoriaRepository) {
        this.repository = repository;
        this.empresaRepository = empresaRepository;
        this.categoriaRepository = categoriaRepository;
    }
 
    @GetMapping("/por-empresa/{empresaId}")
    @Operation(summary = "Listar materiais de interesse da empresa — tela 5.6")
    public ResponseEntity<List<EmpresaMaterialInteresseResponse>> listarPorEmpresa(
            @PathVariable UUID empresaId) {
        return ResponseEntity.ok(
                repository.findByEmpresa_EmpresaId(empresaId).stream()
                        .map(this::toResponse).toList()
        );
    }
 
    @PostMapping
    @Operation(summary = "Adicionar material de interesse")
    public ResponseEntity<EmpresaMaterialInteresseResponse> adicionar(
            @RequestBody @Valid EmpresaMaterialInteresseRequest request) {
        if (repository.existsByEmpresa_EmpresaIdAndCategoria_CategoriaId(
                request.empresaId(), request.categoriaId())) {
            throw new RegraDeNegocioException("Categoria já está nos interesses da empresa.");
        }
        Empresa empresa = empresaRepository.findById(request.empresaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa", request.empresaId()));
        CategoriaMaterial categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("CategoriaMaterial", request.categoriaId()));
        EmpresaMaterialInteresse interesse = new EmpresaMaterialInteresse();
        interesse.setEmpresa(empresa);
        interesse.setCategoria(categoria);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(repository.save(interesse)));
    }
 
    @DeleteMapping("/{id}")
    @Operation(summary = "Remover material de interesse por ID")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("EmpresaMaterialInteresse", id));
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
 
    @DeleteMapping("/por-empresa-categoria")
    @Operation(summary = "Remover material de interesse por empresa e categoria")
    public ResponseEntity<Void> removerPorEmpresaCategoria(
            @RequestParam UUID empresaId,
            @RequestParam UUID categoriaId) {
        EmpresaMaterialInteresse i = repository
                .findByEmpresa_EmpresaIdAndCategoria_CategoriaId(empresaId, categoriaId)
                .orElseThrow(() -> new RegraDeNegocioException("Interesse não encontrado."));
        repository.deleteById(i.getEmpresaMaterialId());
        return ResponseEntity.noContent().build();
    }
 
    private EmpresaMaterialInteresseResponse toResponse(EmpresaMaterialInteresse i) {
        return new EmpresaMaterialInteresseResponse(
                i.getEmpresaMaterialId(),
                i.getEmpresa().getEmpresaId(),
                i.getCategoria().getCategoriaId(),
                i.getCategoria().getNomeCategoria()
        );
    }
}