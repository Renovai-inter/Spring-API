package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.EstoqueRequest;
import com.renovai.api.dto.response.Responses.EstoqueResponse;
import com.renovai.api.service.EstoqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/estoques")
@Tag(name = "Estoques", description = "Controle do estoque de materiais por cooperativa")
public class EstoqueController {

    private final EstoqueService service;

    public EstoqueController(EstoqueService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar estoques", description = "Filtrável por cooperativa e disponibilidade.")
    public ResponseEntity<List<EstoqueResponse>> listar(
            @RequestParam(required = false) Integer cooperativaId,
            @RequestParam(required = false) Boolean apenasDisponiveis) {
        if (cooperativaId != null && Boolean.TRUE.equals(apenasDisponiveis)) {
            return ResponseEntity.ok(service.listarDisponiveisPorCooperativa(cooperativaId));
        }
        if (cooperativaId != null) {
            return ResponseEntity.ok(service.listarPorCooperativa(cooperativaId));
        }
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar estoque por ID")
    public ResponseEntity<EstoqueResponse> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_COOPERATIVA')")
    @Operation(summary = "Criar registro de estoque")
    public ResponseEntity<EstoqueResponse> criar(@RequestBody @Valid EstoqueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_COOPERATIVA')")
    @Operation(summary = "Atualizar quantidade em estoque")
    public ResponseEntity<EstoqueResponse> atualizar(
            @PathVariable Integer id, @RequestBody @Valid EstoqueRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SITE')")
    @Operation(summary = "Excluir registro de estoque")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
