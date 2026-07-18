package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.EquipeCooperadoRequest;
import com.renovai.api.dto.response.Responses.EquipeCooperadoResponse;
import com.renovai.api.service.EquipeCooperadoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/equipe-cooperados")
@Tag(name = "Equipe Cooperados", description = "Vínculo de cooperados a equipes de triagem")
public class EquipeCooperadoController {

    private final EquipeCooperadoService service;

    public EquipeCooperadoController(EquipeCooperadoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar vínculos", description = "Filtrável por equipe ou por cooperado.")
    public ResponseEntity<List<EquipeCooperadoResponse>> listar(
            @RequestParam(required = false) Integer equipeId,
            @RequestParam(required = false) Integer cooperadoId) {
        if (equipeId != null) return ResponseEntity.ok(service.listarPorEquipe(equipeId));
        if (cooperadoId != null) return ResponseEntity.ok(service.listarPorCooperado(cooperadoId));
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar vínculo por ID")
    public ResponseEntity<EquipeCooperadoResponse> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_COOPERATIVA')")
    @Operation(summary = "Adicionar cooperado à equipe",
               description = "Um cooperado pode pertencer a mais de uma equipe. Retorna erro se já estiver vinculado à mesma equipe.")
    public ResponseEntity<EquipeCooperadoResponse> adicionar(@RequestBody @Valid EquipeCooperadoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.adicionar(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_COOPERATIVA')")
    @Operation(summary = "Remover vínculo por ID")
    public ResponseEntity<Void> remover(@PathVariable Integer id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_COOPERATIVA')")
    @Operation(summary = "Remover cooperado de uma equipe",
               description = "Remove pelo par equipeId + cooperadoId, sem precisar do ID do vínculo.")
    public ResponseEntity<Void> removerPorEquipeECooperado(
            @RequestParam Integer equipeId,
            @RequestParam Integer cooperadoId) {
        service.removerPorEquipeECooperado(equipeId, cooperadoId);
        return ResponseEntity.noContent().build();
    }
}