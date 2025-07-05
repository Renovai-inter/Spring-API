package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.*;
import com.renovai.api.dto.response.Responses.*;
import com.renovai.api.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
@Tag(name = "Pedidos", description = "Gestão de pedidos de compra entre empresas e cooperativas")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    // ── Pedidos ──────────────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Listar pedidos", description = "Filtrável por empresa.")
    public ResponseEntity<List<PedidoResponse>> listar(
            @RequestParam(required = false) Integer empresaId) {
        if (empresaId != null) return ResponseEntity.ok(service.listarPorEmpresa(empresaId));
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pedido por ID")
    public ResponseEntity<PedidoResponse> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_EMPRESA')")
    @Operation(summary = "Criar pedido de compra")
    public ResponseEntity<PedidoResponse> criar(@RequestBody @Valid PedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarPedido(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_EMPRESA')")
    @Operation(summary = "Cancelar/excluir pedido")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        service.deletarPedido(id);
        return ResponseEntity.noContent().build();
    }

    // ── Itens do pedido ───────────────────────────────────────────────────────

    @GetMapping("/{pedidoId}/itens")
    @Operation(summary = "Listar itens de um pedido")
    public ResponseEntity<List<ItemResponse>> listarItens(@PathVariable Integer pedidoId) {
        return ResponseEntity.ok(service.listarItensPorPedido(pedidoId));
    }

    @PostMapping("/itens")
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_EMPRESA')")
    @Operation(summary = "Adicionar item ao pedido")
    public ResponseEntity<ItemResponse> adicionarItem(@RequestBody @Valid ItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.adicionarItem(request));
    }

    @DeleteMapping("/itens/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_EMPRESA')")
    @Operation(summary = "Remover item do pedido")
    public ResponseEntity<Void> removerItem(@PathVariable Integer itemId) {
        service.removerItem(itemId);
        return ResponseEntity.noContent().build();
    }

    // ── Pedido × Cooperativa ──────────────────────────────────────────────────

    @GetMapping("/cooperativas")
    @Operation(summary = "Listar pedidos de uma cooperativa",
               description = "Retorna todos os pedidos vinculados a uma cooperativa específica.")
    public ResponseEntity<List<PedidoCooperativaResponse>> listarPorCooperativa(
            @RequestParam Integer cooperativaId) {
        return ResponseEntity.ok(service.listarPorCooperativa(cooperativaId));
    }

    @PostMapping("/cooperativas")
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_COOPERATIVA','GESTOR_EMPRESA')")
    @Operation(summary = "Vincular pedido a uma cooperativa")
    public ResponseEntity<PedidoCooperativaResponse> vincularCooperativa(
            @RequestBody @Valid PedidoCooperativaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.vincularCooperativa(request));
    }

    @PatchMapping("/cooperativas/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_COOPERATIVA')")
    @Operation(summary = "Atualizar status do pedido na cooperativa",
               description = "Gestor aceita, recusa ou finaliza o pedido (altera o status_id).")
    public ResponseEntity<PedidoCooperativaResponse> atualizarStatus(
            @PathVariable Integer id,
            @RequestParam Integer statusId) {
        return ResponseEntity.ok(service.atualizarStatusPedidoCooperativa(id, statusId));
    }
}
