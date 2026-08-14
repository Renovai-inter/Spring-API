package com.renovai.api.controller;
 
import com.renovai.api.dto.request.Requests.EmpresaRequest;
import com.renovai.api.dto.response.Responses.EmpresaDashboardResponse;
import com.renovai.api.dto.response.Responses.EmpresaResponse;
import com.renovai.api.service.EmpresaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
import java.util.UUID;
 
@RestController
@RequestMapping("/empresas")
@Tag(name = "Empresas", description = "Gestão de empresas compradoras")
public class EmpresaController {
 
    private final EmpresaService service;
 
    public EmpresaController(EmpresaService service) {
        this.service = service;
    }
 
    @GetMapping
    @Operation(summary = "Listar todas as empresas")
    public ResponseEntity<List<EmpresaResponse>> listar() {
        return ResponseEntity.ok(service.listarTodas());
    }
 
    @GetMapping("/{id}")
    @Operation(summary = "Buscar empresa por ID")
    public ResponseEntity<EmpresaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }
 
    @GetMapping("/por-nome/{nome}")
    @Operation(summary = "Buscar empresas por nome")
    public ResponseEntity<List<EmpresaResponse>> buscarPorNome(@PathVariable String nome) {
        return ResponseEntity.ok(service.buscarPorNome(nome));
    }
 
    @GetMapping("/{id}/dashboard")
    @Operation(summary = "Dashboard da empresa — tela 5.1")
    public ResponseEntity<EmpresaDashboardResponse> buscarDashboard(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarDashboard(id));
    }
 
    @PostMapping
    @Operation(summary = "Criar empresa")
    public ResponseEntity<EmpresaResponse> criar(@RequestBody @Valid EmpresaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }
 
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar empresa — tela 5.6")
    public ResponseEntity<EmpresaResponse> atualizar(
            @PathVariable UUID id,
            @RequestBody @Valid EmpresaRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }
 
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar empresa")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}