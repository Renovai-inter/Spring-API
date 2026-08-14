 
package com.renovai.api.controller;
 
import com.renovai.api.dto.request.Requests.EquipeRequest;
import com.renovai.api.dto.response.Responses.EquipeResponse;
import com.renovai.api.service.EquipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
import java.util.UUID;
 
@RestController
@RequestMapping("/equipes")
@Tag(name = "Equipes", description = "Gestão de equipes de cooperados — tela 4.8.2")
public class EquipeController {
 
    private final EquipeService service;
 
    public EquipeController(EquipeService service) {
        this.service = service;
    }
 
    @GetMapping
    @Operation(summary = "Listar todas as equipes")
    public ResponseEntity<List<EquipeResponse>> listar() {
        return ResponseEntity.ok(service.listarTodas());
    }
 
    @GetMapping("/ativas")
    @Operation(summary = "Listar equipes ativas")
    public ResponseEntity<List<EquipeResponse>> listarAtivas() {
        return ResponseEntity.ok(service.listarAtivas());
    }
 
    @GetMapping("/por-cooperativa/{cooperativaId}")
    @Operation(summary = "Listar equipes da cooperativa")
    public ResponseEntity<List<EquipeResponse>> listarPorCooperativa(@PathVariable UUID cooperativaId) {
        return ResponseEntity.ok(service.listarPorCooperativa(cooperativaId));
    }
 
    @GetMapping("/{id}")
    @Operation(summary = "Buscar equipe por ID")
    public ResponseEntity<EquipeResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }
 
    @PostMapping
    @Operation(summary = "Criar equipe")
    public ResponseEntity<EquipeResponse> criar(@RequestBody @Valid EquipeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }
 
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar equipe")
    public ResponseEntity<EquipeResponse> atualizar(
            @PathVariable UUID id, @RequestBody @Valid EquipeRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }
 
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar equipe")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}