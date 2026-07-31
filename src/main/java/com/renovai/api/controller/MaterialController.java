package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.MaterialRequest;
import com.renovai.api.dto.response.Responses.MaterialResponse;
import com.renovai.api.service.MaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/materiais")
@Tag(name = "Materiais", description = "Gestão de materiais recicláveis")
public class MaterialController {
    private final MaterialService service;
    public MaterialController(MaterialService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<MaterialResponse>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/disponiveis")
    public ResponseEntity<List<MaterialResponse>> listarDisponiveis() {
        return ResponseEntity.ok(service.listarDisponiveis());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/por-categoria")
    public ResponseEntity<List<MaterialResponse>> buscarPorCategoria(
            @RequestParam(required = false) UUID categoriaId) {
        return ResponseEntity.ok(service.buscarPorCategoria(categoriaId));
    }

    @PostMapping
    public ResponseEntity<MaterialResponse> criar(@RequestBody @Valid MaterialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaterialResponse> atualizar(
            @PathVariable UUID id, @RequestBody @Valid MaterialRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    
}