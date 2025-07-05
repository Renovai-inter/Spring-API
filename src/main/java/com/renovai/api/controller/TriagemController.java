package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.TriagemRequest;
import com.renovai.api.dto.response.Responses.TriagemResponse;
import com.renovai.api.service.TriagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/triagens")
@Tag(name = "Triagens", description = "Registro da separação de materiais por tipo")
public class TriagemController {

    private final TriagemService service;

    public TriagemController(TriagemService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar triagens", description = "Filtrável por coleta.")
    public ResponseEntity<List<TriagemResponse>> listar(
            @RequestParam(required = false) Integer coletaId) {
        if (coletaId != null) return ResponseEntity.ok(service.listarPorColeta(coletaId));
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar triagem por ID")
    public ResponseEntity<TriagemResponse> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_COOPERATIVA','FUNCIONARIO_COOPERATIVA')")
    @Operation(summary = "Registrar triagem",
               description = "Cria triagem e incrementa automaticamente o estoque da cooperativa.")
    public ResponseEntity<TriagemResponse> criar(@RequestBody @Valid TriagemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_COOPERATIVA','FUNCIONARIO_COOPERATIVA')")
    @Operation(summary = "Atualizar triagem")
    public ResponseEntity<TriagemResponse> atualizar(
            @PathVariable Integer id, @RequestBody @Valid TriagemRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_COOPERATIVA')")
    @Operation(summary = "Excluir triagem")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
