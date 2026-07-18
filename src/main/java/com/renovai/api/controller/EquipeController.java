package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.EquipeRequest;
import com.renovai.api.dto.response.Responses.EquipeResponse;
import com.renovai.api.service.EquipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/equipes")
@Tag(name = "Equipes", description = "Gestão de equipes de triagem das cooperativas")
public class EquipeController {

    private final EquipeService service;

    public EquipeController(EquipeService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar equipes", description = "Filtrável por cooperativa ou apenas ativas.")
    public ResponseEntity<List<EquipeResponse>> listar(
            @RequestParam(required = false) Integer cooperativaId,
            @RequestParam(required = false, defaultValue = "false") boolean apenasAtivas) {
        if (cooperativaId != null) return ResponseEntity.ok(service.listarPorCooperativa(cooperativaId));
        if (apenasAtivas) return ResponseEntity.ok(service.listarAtivas());
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar equipe por ID")
    public ResponseEntity<EquipeResponse> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_COOPERATIVA')")
    @Operation(summary = "Criar equipe", description = "Cria uma nova equipe vinculada a uma cooperativa e um gestor.")
    public ResponseEntity<EquipeResponse> criar(@RequestBody @Valid EquipeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_COOPERATIVA')")
    @Operation(summary = "Atualizar equipe")
    public ResponseEntity<EquipeResponse> atualizar(
            @PathVariable Integer id, @RequestBody @Valid EquipeRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_COOPERATIVA')")
    @Operation(summary = "Excluir equipe")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}