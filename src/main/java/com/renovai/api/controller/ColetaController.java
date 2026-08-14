package com.renovai.api.controller;
 
import com.renovai.api.dto.request.Requests.ColetaRequest;
import com.renovai.api.dto.request.Requests.AtualizarStatusColetaRequest;
import com.renovai.api.dto.response.Responses.ColetaResponse;
import com.renovai.api.service.ColetaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
import java.util.UUID;
 
@RestController
@RequestMapping("/coletas")
@Tag(name = "Coletas", description = "Registro de coletas de materiais — telas 2.2, 4.2")
public class ColetaController {
 
    private final ColetaService service;
 
    public ColetaController(ColetaService service) {
        this.service = service;
    }
 
    @GetMapping
    @Operation(summary = "Listar todas as coletas")
    public ResponseEntity<List<ColetaResponse>> listar() {
        return ResponseEntity.ok(service.listarTodas());
    }
 
    @GetMapping("/{id}")
    @Operation(summary = "Buscar coleta por ID — tela 4.2.1")
    public ResponseEntity<ColetaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }
 
    @GetMapping("/por-cooperado/{cooperadoId}")
    @Operation(summary = "Listar coletas por cooperado — tela 2.6")
    public ResponseEntity<List<ColetaResponse>> listarPorCooperado(@PathVariable UUID cooperadoId) {
        return ResponseEntity.ok(service.listarPorCooperado(cooperadoId));
    }
 
    @GetMapping("/por-cooperativa/{cooperativaId}")
    @Operation(summary = "Listar coletas da cooperativa — tela 4.2")
    public ResponseEntity<List<ColetaResponse>> listarPorCooperativa(@PathVariable UUID cooperativaId) {
        return ResponseEntity.ok(service.listarPorCooperativa(cooperativaId));
    }
 
    @GetMapping("/por-cooperativa/{cooperativaId}/tipo/{tipoColeta}")
    @Operation(summary = "Listar coletas por cooperativa e tipo (INTERNO/EXTERNO)")
    public ResponseEntity<List<ColetaResponse>> listarPorCooperativaETipo(
            @PathVariable UUID cooperativaId,
            @PathVariable String tipoColeta) {
        return ResponseEntity.ok(service.listarPorCooperativaETipo(cooperativaId, tipoColeta));
    }
 
    @GetMapping("/por-cooperativa/{cooperativaId}/status/{status}")
    @Operation(summary = "Listar coletas por cooperativa e status")
    public ResponseEntity<List<ColetaResponse>> listarPorCooperativaEStatus(
            @PathVariable UUID cooperativaId,
            @PathVariable String status) {
        return ResponseEntity.ok(service.listarPorCooperativaEStatus(cooperativaId, status));
    }
 
    @GetMapping("/por-rota/{rotaId}")
    @Operation(summary = "Listar coletas vinculadas a uma rota")
    public ResponseEntity<List<ColetaResponse>> listarPorRota(@PathVariable UUID rotaId) {
        return ResponseEntity.ok(service.listarPorRota(rotaId));
    }
 
    @PostMapping
    @Operation(summary = "Registrar nova coleta — tela 2.2")
    public ResponseEntity<ColetaResponse> criar(@RequestBody @Valid ColetaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }
 
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar coleta")
    public ResponseEntity<ColetaResponse> atualizar(
            @PathVariable UUID id,
            @RequestBody @Valid ColetaRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }
 
    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status da coleta")
    public ResponseEntity<ColetaResponse> atualizarStatus(
            @PathVariable UUID id,
            @RequestBody @Valid AtualizarStatusColetaRequest request) {
        return ResponseEntity.ok(service.atualizarStatus(id, request));
    }
 
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar coleta")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}