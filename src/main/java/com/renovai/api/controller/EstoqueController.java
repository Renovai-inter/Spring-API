package com.renovai.api.controller;
 
import com.renovai.api.dto.request.Requests.EstoqueRequest;
import com.renovai.api.dto.request.Requests.AtualizarQuantidadeEstoqueRequest;
import com.renovai.api.dto.response.Responses.EstoqueResponse;
import com.renovai.api.service.EstoqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
import java.util.UUID;
 
@RestController
@RequestMapping("/estoques")
@Tag(name = "Estoques", description = "Gestão de estoque de materiais — tela 4.3")
public class EstoqueController {
 
    private final EstoqueService service;
 
    public EstoqueController(EstoqueService service) {
        this.service = service;
    }
 
    @GetMapping
    @Operation(summary = "Listar todos os estoques")
    public ResponseEntity<List<EstoqueResponse>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }
 
    @GetMapping("/{id}")
    @Operation(summary = "Buscar estoque por ID")
    public ResponseEntity<EstoqueResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }
 
    @GetMapping("/por-cooperativa/{cooperativaId}")
    @Operation(summary = "Listar estoque da cooperativa — tela 4.3")
    public ResponseEntity<List<EstoqueResponse>> listarPorCooperativa(
            @PathVariable UUID cooperativaId) {
        return ResponseEntity.ok(service.listarPorCooperativa(cooperativaId));
    }
 
    @GetMapping("/disponiveis/por-cooperativa/{cooperativaId}")
    @Operation(summary = "Listar estoque disponível da cooperativa (quantidade > 0)")
    public ResponseEntity<List<EstoqueResponse>> listarDisponiveisPorCooperativa(
            @PathVariable UUID cooperativaId) {
        return ResponseEntity.ok(service.listarDisponiveisPorCooperativa(cooperativaId));
    }
 
    @PostMapping
    @Operation(summary = "Criar entrada de estoque")
    public ResponseEntity<EstoqueResponse> criar(@RequestBody @Valid EstoqueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }
 
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar estoque")
    public ResponseEntity<EstoqueResponse> atualizar(
            @PathVariable UUID id,
            @RequestBody @Valid EstoqueRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }
 
    @PatchMapping("/{id}/quantidade")
    @Operation(summary = "Ajustar quantidade do estoque manualmente")
    public ResponseEntity<EstoqueResponse> atualizarQuantidade(
            @PathVariable UUID id,
            @RequestBody @Valid AtualizarQuantidadeEstoqueRequest request) {
        return ResponseEntity.ok(service.atualizarQuantidade(id, request));
    }
 
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar estoque")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}