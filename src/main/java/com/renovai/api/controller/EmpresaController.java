package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.EmpresaRequest;
import com.renovai.api.dto.response.Responses.EmpresaResponse;
import com.renovai.api.service.EmpresaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/empresas")
@Tag(name = "Empresas", description = "Cadastro de empresas compradoras e geradoras de resíduos")
public class EmpresaController {

    private final EmpresaService service;

    public EmpresaController(EmpresaService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar todas as empresas")
    public ResponseEntity<List<EmpresaResponse>> listar(@RequestParam(required = false) String nome) {
        if (nome != null) return ResponseEntity.ok(service.buscarPorNome(nome));
        return ResponseEntity.ok(service.listarTodas());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar empresa por ID")
    public ResponseEntity<EmpresaResponse> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_SITE', 'ADMIN_COOPERATIVA')")
    @Operation(summary = "Cadastrar nova empresa")
    public ResponseEntity<EmpresaResponse> criar(@RequestBody @Valid EmpresaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SITE', 'ADMIN_COOPERATIVA')")
    @Operation(summary = "Atualizar empresa")
    public ResponseEntity<EmpresaResponse> atualizar(
            @PathVariable Integer id, @RequestBody @Valid EmpresaRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SITE')")
    @Operation(summary = "Excluir empresa")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
