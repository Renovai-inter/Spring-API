package com.renovai.api.controller;
 
import com.renovai.api.dto.request.Requests.PedidoRequest;
import com.renovai.api.dto.request.Requests.ItemRequest;
import com.renovai.api.dto.request.Requests.PedidoCooperativaRequest;
import com.renovai.api.dto.response.Responses.PedidoResponse;
import com.renovai.api.dto.response.Responses.ItemResponse;
import com.renovai.api.dto.response.Responses.PedidoCooperativaResponse;
import com.renovai.api.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
import java.util.UUID;
 
@RestController
@RequestMapping("/pedidos")
@Tag(name = "Pedidos", description = "Gestão de pedidos de compra — telas 5.3, 5.4, 4.5")
public class PedidoController {
 
    private final PedidoService service;
 
    public PedidoController(PedidoService service) {
        this.service = service;
    }
 
    @GetMapping
    @Operation(summary = "Listar todos os pedidos")
    public ResponseEntity<List<PedidoResponse>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }
 
    @GetMapping("/{id}")
    @Operation(summary = "Buscar pedido por ID — tela 5.4.1 e 4.5.1")
    public ResponseEntity<PedidoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }
 
    @GetMapping("/por-empresa/{empresaId}")
    @Operation(summary = "Listar pedidos da empresa — tela 5.4")
    public ResponseEntity<List<PedidoResponse>> listarPorEmpresa(@PathVariable UUID empresaId) {
        return ResponseEntity.ok(service.listarPorEmpresa(empresaId));
    }
 
    @PostMapping
    @Operation(summary = "Criar pedido — tela 5.3")
    public ResponseEntity<PedidoResponse> criar(@RequestBody @Valid PedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarPedido(request));
    }
 
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar pedido")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletarPedido(id);
        return ResponseEntity.noContent().build();
    }
 
    @GetMapping("/{pedidoId}/itens")
    @Operation(summary = "Listar itens do pedido")
    public ResponseEntity<List<ItemResponse>> listarItens(@PathVariable UUID pedidoId) {
        return ResponseEntity.ok(service.listarItensPorPedido(pedidoId));
    }
 
    @PostMapping("/itens")
    @Operation(summary = "Adicionar item ao pedido")
    public ResponseEntity<ItemResponse> adicionarItem(@RequestBody @Valid ItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.adicionarItem(request));
    }
 
    @DeleteMapping("/itens/{itemId}")
    @Operation(summary = "Remover item do pedido")
    public ResponseEntity<Void> removerItem(@PathVariable UUID itemId) {
        service.removerItem(itemId);
        return ResponseEntity.noContent().build();
    }
 
    @GetMapping("/por-cooperativa/{cooperativaId}")
    @Operation(summary = "Listar pedidos da cooperativa — tela 4.5")
    public ResponseEntity<List<PedidoCooperativaResponse>> listarPorCooperativa(
            @PathVariable UUID cooperativaId) {
        return ResponseEntity.ok(service.listarPorCooperativa(cooperativaId));
    }
 
    @PostMapping("/cooperativa")
    @Operation(summary = "Vincular pedido à cooperativa")
    public ResponseEntity<PedidoCooperativaResponse> vincularCooperativa(
            @RequestBody @Valid PedidoCooperativaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.vincularCooperativa(request));
    }
 
    @PutMapping("/cooperativa/{id}/status/{statusId}")
    @Operation(summary = "Atualizar status do pedido na cooperativa")
    public ResponseEntity<PedidoCooperativaResponse> atualizarStatus(
            @PathVariable UUID id, @PathVariable UUID statusId) {
        return ResponseEntity.ok(service.atualizarStatusPedidoCooperativa(id, statusId));
    }
}