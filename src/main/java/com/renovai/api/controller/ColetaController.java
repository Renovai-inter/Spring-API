package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.ColetaRequest;
import com.renovai.api.dto.response.Responses.ColetaResponse;
import com.renovai.api.service.ColetaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/coletas")
@Tag(name = "Coletas", description = "Registro de coletas de materiais recicláveis")
public class ColetaController {

    private final ColetaService service;

    public ColetaController(ColetaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar todas as coletas")
    public ResponseEntity<List<ColetaResponse>> listar(
            @RequestParam(required = false) Integer cooperadoId) {
        if (cooperadoId != null) return ResponseEntity.ok(service.listarPorCooperado(cooperadoId));
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar coleta por ID")
    public ResponseEntity<ColetaResponse> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_COOPERATIVA','FUNCIONARIO_COOPERATIVA')")
    @Operation(summary = "Registrar nova coleta")
    public ResponseEntity<ColetaResponse> criar(@RequestBody @Valid ColetaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_COOPERATIVA','FUNCIONARIO_COOPERATIVA')")
    @Operation(summary = "Atualizar coleta")
    public ResponseEntity<ColetaResponse> atualizar(
            @PathVariable Integer id, @RequestBody @Valid ColetaRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_COOPERATIVA')")
    @Operation(summary = "Excluir coleta")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
