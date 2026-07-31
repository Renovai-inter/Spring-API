package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.CooperativaRequest;
import com.renovai.api.dto.response.Responses.CooperativaResponse;
import com.renovai.api.service.CooperativaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/cooperativas")
@Tag(name = "Cooperativas", description = "Gestão de cooperativas de reciclagem")
public class CooperativaController {
    private final CooperativaService service;
    public CooperativaController(CooperativaService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<CooperativaResponse>> listar() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CooperativaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/por-nome/{nome}")
    public ResponseEntity<List<CooperativaResponse>> buscarPorNome(@PathVariable String nome) {
        return ResponseEntity.ok(service.buscarPorNome(nome));
    }

    @PostMapping
    public ResponseEntity<CooperativaResponse> criar(@RequestBody @Valid CooperativaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CooperativaResponse> atualizar(
            @PathVariable UUID id, @RequestBody @Valid CooperativaRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}