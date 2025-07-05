package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.MaterialRequest;
import com.renovai.api.dto.response.Responses.MaterialResponse;
import com.renovai.api.service.MaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/materiais")
@Tag(name = "Materiais", description = "Cadastro e consulta de materiais recicláveis")
public class MaterialController {

    private final MaterialService service;

    public MaterialController(MaterialService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar todos os materiais",
               description = "Filtrar por categoria ou listar apenas disponíveis.")
    public ResponseEntity<List<MaterialResponse>> listar(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean apenasDisponiveis) {
        if (categoria != null) return ResponseEntity.ok(service.buscarPorCategoria(categoria));
        if (Boolean.TRUE.equals(apenasDisponiveis)) return ResponseEntity.ok(service.listarDisponiveis());
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar material por ID")
    public ResponseEntity<MaterialResponse> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_SITE', 'GESTOR_COOPERATIVA')")
    @Operation(summary = "Cadastrar novo material")
    public ResponseEntity<MaterialResponse> criar(@RequestBody @Valid MaterialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SITE', 'GESTOR_COOPERATIVA')")
    @Operation(summary = "Atualizar material")
    public ResponseEntity<MaterialResponse> atualizar(
            @PathVariable Integer id, @RequestBody @Valid MaterialRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SITE')")
    @Operation(summary = "Excluir material")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
