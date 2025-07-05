package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.CooperativaRequest;
import com.renovai.api.dto.response.Responses.CooperativaResponse;
import com.renovai.api.service.CooperativaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/cooperativas")
@Tag(name = "Cooperativas", description = "Gestão das cooperativas de reciclagem")
public class CooperativaController {

    private final CooperativaService service;

    public CooperativaController(CooperativaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar todas as cooperativas")
    public ResponseEntity<List<CooperativaResponse>> listar(
            @RequestParam(required = false) @Parameter(description = "Filtrar por nome") String nome) {
        if (nome != null && !nome.isBlank()) {
            return ResponseEntity.ok(service.buscarPorNome(nome));
        }
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cooperativa por ID")
    @ApiResponse(responseCode = "404", description = "Cooperativa não encontrada")
    public ResponseEntity<CooperativaResponse> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_SITE', 'ADMIN_COOPERATIVA')")
    @Operation(summary = "Cadastrar nova cooperativa")
    public ResponseEntity<CooperativaResponse> criar(@RequestBody @Valid CooperativaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SITE', 'ADMIN_COOPERATIVA')")
    @Operation(summary = "Atualizar cooperativa")
    public ResponseEntity<CooperativaResponse> atualizar(
            @PathVariable Integer id,
            @RequestBody @Valid CooperativaRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SITE')")
    @Operation(summary = "Excluir cooperativa")
    @ApiResponse(responseCode = "204", description = "Excluída com sucesso")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
