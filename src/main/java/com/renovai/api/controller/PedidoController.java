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
@Tag(name = "Pedidos", description = "Gestão de pedidos de compra")
public class PedidoController {
    private final PedidoService service;
    public PedidoController(PedidoService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<PedidoResponse>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/por-empresa/{empresaId}")
    public ResponseEntity<List<PedidoResponse>> listarPorEmpresa(@PathVariable UUID empresaId) {
        return ResponseEntity.ok(service.listarPorEmpresa(empresaId));
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@RequestBody @Valid PedidoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criarPedido(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletarPedido(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{pedidoId}/itens")
    public ResponseEntity<List<ItemResponse>> listarItens(@PathVariable UUID pedidoId) {
        return ResponseEntity.ok(service.listarItensPorPedido(pedidoId));
    }

    @PostMapping("/itens")
    public ResponseEntity<ItemResponse> adicionarItem(@RequestBody @Valid ItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.adicionarItem(request));
    }

    @DeleteMapping("/itens/{itemId}")
    public ResponseEntity<Void> removerItem(@PathVariable UUID itemId) {
        service.removerItem(itemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/por-cooperativa/{cooperativaId}")
    public ResponseEntity<List<PedidoCooperativaResponse>> listarPorCooperativa(
            @PathVariable UUID cooperativaId) {
        return ResponseEntity.ok(service.listarPorCooperativa(cooperativaId));
    }

    @PostMapping("/cooperativa")
    public ResponseEntity<PedidoCooperativaResponse> vincularCooperativa(
            @RequestBody @Valid PedidoCooperativaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.vincularCooperativa(request));
    }

    @PutMapping("/cooperativa/{id}/status/{statusId}")
    public ResponseEntity<PedidoCooperativaResponse> atualizarStatus(
            @PathVariable UUID id, @PathVariable UUID statusId) {
        return ResponseEntity.ok(service.atualizarStatusPedidoCooperativa(id, statusId));
    }
}