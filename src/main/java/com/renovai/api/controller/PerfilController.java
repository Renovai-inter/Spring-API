package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.PerfilRequest;
import com.renovai.api.dto.response.Responses.PerfilResponse;
import com.renovai.api.service.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/perfis")
@Tag(name = "Perfis", description = "Gestão de perfis de usuário")
public class PerfilController {

    private final PerfilService service;

    public PerfilController(PerfilService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar todos os perfis")
    public ResponseEntity<List<PerfilResponse>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/ativos")
    @Operation(summary = "Listar perfis ativos")
    public ResponseEntity<List<PerfilResponse>> listarAtivos() {
        return ResponseEntity.ok(service.listarAtivos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar perfil por ID")
    public ResponseEntity<PerfilResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_SITE')")
    @Operation(summary = "Criar novo perfil")
    public ResponseEntity<PerfilResponse> criar(@RequestBody @Valid PerfilRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar perfil")
    public ResponseEntity<PerfilResponse> atualizar(
            @PathVariable UUID id, @RequestBody @Valid PerfilRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @PutMapping("/{id}/desativar")
    @Operation(summary = "Desativar perfil")
    public ResponseEntity<PerfilResponse> desativar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.desativar(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SITE')")
    @Operation(summary = "Deletar perfil")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}