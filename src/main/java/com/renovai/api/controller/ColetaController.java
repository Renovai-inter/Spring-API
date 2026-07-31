package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.ColetaRequest;
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
@Tag(name = "Coletas", description = "Registro de coletas de materiais recicláveis")
public class ColetaController {
    private final ColetaService service;
    public ColetaController(ColetaService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<ColetaResponse>> listar() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ColetaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/por-cooperado/{cooperadoId}")
    public ResponseEntity<List<ColetaResponse>> listarPorCooperado(
            @PathVariable UUID cooperadoId) {
        return ResponseEntity.ok(service.listarPorCooperado(cooperadoId));
    }

    @PostMapping
    public ResponseEntity<ColetaResponse> criar(@RequestBody @Valid ColetaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ColetaResponse> atualizar(
            @PathVariable UUID id, @RequestBody @Valid ColetaRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}