package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.TriagemRequest;
import com.renovai.api.dto.response.Responses.TriagemResponse;
import com.renovai.api.service.TriagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/triagens")
@Tag(name = "Triagens", description = "Registro de triagens de materiais recicláveis")
public class TriagemController {
    private final TriagemService service;
    public TriagemController(TriagemService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<TriagemResponse>> listar() {
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TriagemResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/por-coleta/{coletaId}")
    public ResponseEntity<List<TriagemResponse>> listarPorColeta(
            @PathVariable UUID coletaId) {
        return ResponseEntity.ok(service.listarPorColeta(coletaId));
    }

    @PostMapping
    public ResponseEntity<TriagemResponse> criar(@RequestBody @Valid TriagemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TriagemResponse> atualizar(
            @PathVariable UUID id, @RequestBody @Valid TriagemRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}