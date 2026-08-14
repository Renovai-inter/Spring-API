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
@Tag(name = "Equipes Cooperados", description = "Vínculo entre equipes e cooperados — tela 4.8.2")
public class EquipeCooperadoController {
 
    private final EquipeCooperadoService service;
 
    public EquipeCooperadoController(EquipeCooperadoService service) {
        this.service = service;
    }
 
    @GetMapping
    @Operation(summary = "Listar todos os vínculos equipe-cooperado")
    public ResponseEntity<List<EquipeCooperadoResponse>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }
 
    @GetMapping("/por-equipe/{equipeId}")
    @Operation(summary = "Listar cooperados de uma equipe")
    public ResponseEntity<List<EquipeCooperadoResponse>> listarPorEquipe(@PathVariable UUID equipeId) {
        return ResponseEntity.ok(service.listarPorEquipe(equipeId));
    }
 
    @GetMapping("/por-cooperado/{cooperadoId}")
    @Operation(summary = "Listar equipes de um cooperado")
    public ResponseEntity<List<EquipeCooperadoResponse>> listarPorCooperado(
            @PathVariable UUID cooperadoId) {
        return ResponseEntity.ok(service.listarPorCooperado(cooperadoId));
    }
 
    @GetMapping("/{id}")
    @Operation(summary = "Buscar vínculo por ID")
    public ResponseEntity<EquipeCooperadoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }
 
    @PostMapping
    @Operation(summary = "Adicionar cooperado à equipe")
    public ResponseEntity<EquipeCooperadoResponse> adicionar(
            @RequestBody @Valid EquipeCooperadoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.adicionar(request));
    }
 
    @DeleteMapping("/{id}")
    @Operation(summary = "Remover vínculo por ID")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
 
    @DeleteMapping("/remover-por-equipe-cooperado")
    @Operation(summary = "Remover cooperado da equipe por IDs")
    public ResponseEntity<Void> removerPorEquipeECooperado(
            @RequestParam UUID equipeId, @RequestParam UUID cooperadoId) {
        service.removerPorEquipeECooperado(equipeId, cooperadoId);
        return ResponseEntity.noContent().build();
    }
}