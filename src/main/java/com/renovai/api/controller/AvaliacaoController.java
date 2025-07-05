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

@RestController
@RequestMapping("/avaliacoes")
@Tag(name = "Avaliações", description = "Sistema de avaliação entre empresas e cooperativas")
public class AvaliacaoController {

    private final AvaliacaoService service;

    public AvaliacaoController(AvaliacaoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar avaliações", description = "Filtrável por perfil avaliado.")
    public ResponseEntity<List<AvaliacaoResponse>> listar(
            @RequestParam(required = false) Integer avaliadoId) {
        if (avaliadoId != null) return ResponseEntity.ok(service.listarPorAvaliado(avaliadoId));
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar avaliação por ID")
    public ResponseEntity<AvaliacaoResponse> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/media/{perfilId}")
    @Operation(summary = "Média de notas de um perfil")
    public ResponseEntity<Double> mediaNota(@PathVariable Integer perfilId) {
        return ResponseEntity.ok(service.mediaNotasPorPerfil(perfilId));
    }

    @PostMapping
    @Operation(summary = "Criar avaliação")
    public ResponseEntity<AvaliacaoResponse> criar(@RequestBody @Valid AvaliacaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SITE')")
    @Operation(summary = "Excluir avaliação")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
