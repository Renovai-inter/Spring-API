package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.AvaliacaoRequest;
import com.renovai.api.dto.response.Responses.AvaliacaoResponse;
import com.renovai.api.service.AvaliacaoService;
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
@RequestMapping("/avaliacoes")
@Tag(name = "Avaliações", description = "Sistema de avaliação entre empresas e cooperativas")
public class AvaliacaoController {
    private final AvaliacaoService service;
    public AvaliacaoController(AvaliacaoService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<AvaliacaoResponse>> listar(
            @RequestParam(required = false) UUID avaliadoId) {
        if (avaliadoId != null) return ResponseEntity.ok(service.listarPorAvaliado(avaliadoId));
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AvaliacaoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/media/{perfilId}")
    public ResponseEntity<Double> mediaNota(@PathVariable UUID perfilId) {
        return ResponseEntity.ok(service.mediaNotasPorPerfil(perfilId));
    }

    @PostMapping
    public ResponseEntity<AvaliacaoResponse> criar(@RequestBody @Valid AvaliacaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @GetMapping("/por-pedido/{pedidoId}")
    public ResponseEntity<List<AvaliacaoResponse>> listarPorPedido(@PathVariable UUID pedidoId) {
        return ResponseEntity.ok(service.listarPorPedido(pedidoId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SITE')")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}