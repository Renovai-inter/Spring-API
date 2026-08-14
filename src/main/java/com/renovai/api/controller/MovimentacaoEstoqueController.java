package com.renovai.api.controller;
 
import com.renovai.api.dto.response.Responses.MovimentacaoEstoqueResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.MovimentacaoEstoque;
import com.renovai.api.repository.MovimentacaoEstoqueRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
 
@RestController
@RequestMapping("/movimentacoes-estoque")
@Tag(name = "Movimentações de Estoque", description = "Histórico de entradas e saídas de estoque")
public class MovimentacaoEstoqueController {
 
    private final MovimentacaoEstoqueRepository repository;
 
    public MovimentacaoEstoqueController(MovimentacaoEstoqueRepository repository) {
        this.repository = repository;
    }
 
    @GetMapping("/por-cooperativa/{cooperativaId}")
    @Operation(summary = "Listar movimentações da cooperativa")
    public ResponseEntity<List<MovimentacaoEstoqueResponse>> listarPorCooperativa(
            @PathVariable UUID cooperativaId) {
        return ResponseEntity.ok(repository.findByCooperativa(cooperativaId)
                .stream().map(this::toResponse).toList());
    }
 
    @GetMapping("/por-cooperativa/{cooperativaId}/periodo")
    @Operation(summary = "Listar movimentações da cooperativa por período")
    public ResponseEntity<List<MovimentacaoEstoqueResponse>> listarPorCooperativaEPeriodo(
            @PathVariable UUID cooperativaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fim) {
        return ResponseEntity.ok(repository.findByCooperativaAndPeriodo(cooperativaId, inicio, fim)
                .stream().map(this::toResponse).toList());
    }
 
    @GetMapping("/por-cooperativa/{cooperativaId}/entradas")
    @Operation(summary = "Listar entradas de estoque da cooperativa")
    public ResponseEntity<List<MovimentacaoEstoqueResponse>> listarEntradas(
            @PathVariable UUID cooperativaId) {
        return ResponseEntity.ok(repository.findEntradasByCooperativa(cooperativaId)
                .stream().map(this::toResponse).toList());
    }
 
    @GetMapping("/por-cooperativa/{cooperativaId}/saidas")
    @Operation(summary = "Listar saídas de estoque da cooperativa")
    public ResponseEntity<List<MovimentacaoEstoqueResponse>> listarSaidas(
            @PathVariable UUID cooperativaId) {
        return ResponseEntity.ok(repository.findSaidasByCooperativa(cooperativaId)
                .stream().map(this::toResponse).toList());
    }
 
    @GetMapping("/por-estoque/{estoqueId}")
    @Operation(summary = "Listar movimentações de um item de estoque específico")
    public ResponseEntity<List<MovimentacaoEstoqueResponse>> listarPorEstoque(
            @PathVariable UUID estoqueId) {
        return ResponseEntity.ok(
                repository.findByEstoque_EstoqueIdOrderByDataMovimentacaoDesc(estoqueId)
                        .stream().map(this::toResponse).toList());
    }
 
    @GetMapping("/{id}")
    @Operation(summary = "Buscar movimentação por ID")
    public ResponseEntity<MovimentacaoEstoqueResponse> buscarPorId(@PathVariable UUID id) {
        MovimentacaoEstoque m = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("MovimentacaoEstoque", id));
        return ResponseEntity.ok(toResponse(m));
    }
 
    private MovimentacaoEstoqueResponse toResponse(MovimentacaoEstoque m) {
        return new MovimentacaoEstoqueResponse(
                m.getMovimentacaoId(),
                m.getEstoque().getEstoqueId(),
                m.getEstoque().getCooperativa().getCooperativaId(),
                m.getEstoque().getCooperativa().getNome(),
                m.getEstoque().getMaterial().getMaterialId(),
                m.getEstoque().getMaterial().getCategoria() != null
                        ? m.getEstoque().getMaterial().getCategoria().getNomeCategoria() : null,
                m.getTriagem() != null ? m.getTriagem().getEventoId() : null,
                m.getItem() != null ? m.getItem().getItemId() : null,
                m.getQuantidadeKg(),
                m.getTipoMovimentacao(),
                m.getDataMovimentacao()
        );
    }
}