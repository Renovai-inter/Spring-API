package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.EquipeCooperadoRequest;
import com.renovai.api.dto.response.Responses.EquipeCooperadoResponse;
import com.renovai.api.service.EquipeCooperadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/equipes-cooperados")
@Tag(name = "Equipes Cooperados", description = "Vínculo entre equipes e cooperados")
public class EquipeCooperadoController {
    private final EquipeCooperadoService service;
    public EquipeCooperadoController(EquipeCooperadoService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<EquipeCooperadoResponse>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/por-equipe/{equipeId}")
    public ResponseEntity<List<EquipeCooperadoResponse>> listarPorEquipe(
            @PathVariable UUID equipeId) {
        return ResponseEntity.ok(service.listarPorEquipe(equipeId));
    }

    @GetMapping("/por-cooperado/{cooperadoId}")
    public ResponseEntity<List<EquipeCooperadoResponse>> listarPorCooperado(
            @PathVariable UUID cooperadoId) {
        return ResponseEntity.ok(service.listarPorCooperado(cooperadoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipeCooperadoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<EquipeCooperadoResponse> adicionar(
            @RequestBody @Valid EquipeCooperadoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.adicionar(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/remover-por-equipe-cooperado")
    public ResponseEntity<Void> removerPorEquipeECooperado(
            @RequestParam UUID equipeId,
            @RequestParam UUID cooperadoId) {
        service.removerPorEquipeECooperado(equipeId, cooperadoId);
        return ResponseEntity.noContent().build();
    }
}