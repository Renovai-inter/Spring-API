package com.renovai.api.controller;
 
import com.renovai.api.dto.request.Requests.TriagemRequest;
import com.renovai.api.dto.request.Requests.AtualizarStatusTriagemRequest;
import com.renovai.api.dto.request.Requests.ConcluirTriagemRequest;
import com.renovai.api.dto.response.Responses.TriagemResponse;
import com.renovai.api.service.TriagemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
import java.util.UUID;
 
@RestController
@RequestMapping("/triagens")
@Tag(name = "Triagens", description = "Registro de triagens de materiais — telas 2.3, 4.8")
public class TriagemController {
 
    private final TriagemService service;
 
    public TriagemController(TriagemService service) {
        this.service = service;
    }
 
    @GetMapping
    @Operation(summary = "Listar todas as triagens")
    public ResponseEntity<List<TriagemResponse>> listar() {
        return ResponseEntity.ok(service.listarTodas());
    }
 
    @GetMapping("/{id}")
    @Operation(summary = "Buscar triagem por ID — tela 4.8.1")
    public ResponseEntity<TriagemResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }
 
    @GetMapping("/por-coleta/{coletaId}")
    @Operation(summary = "Listar triagens de uma coleta")
    public ResponseEntity<List<TriagemResponse>> listarPorColeta(@PathVariable UUID coletaId) {
        return ResponseEntity.ok(service.listarPorColeta(coletaId));
    }
 
    @GetMapping("/por-cooperativa/{cooperativaId}")
    @Operation(summary = "Listar triagens da cooperativa — tela 4.8")
    public ResponseEntity<List<TriagemResponse>> listarPorCooperativa(@PathVariable UUID cooperativaId) {
        return ResponseEntity.ok(service.listarPorCooperativa(cooperativaId));
    }
 
    @GetMapping("/por-cooperativa/{cooperativaId}/status/{status}")
    @Operation(summary = "Listar triagens da cooperativa por status")
    public ResponseEntity<List<TriagemResponse>> listarPorCooperativaEStatus(
            @PathVariable UUID cooperativaId,
            @PathVariable String status) {
        return ResponseEntity.ok(service.listarPorCooperativaEStatus(cooperativaId, status));
    }
 
    @GetMapping("/por-cooperado/{cooperadoId}")
    @Operation(summary = "Listar triagens do cooperado — tela 2.7")
    public ResponseEntity<List<TriagemResponse>> listarPorCooperado(@PathVariable UUID cooperadoId) {
        return ResponseEntity.ok(service.listarPorCooperado(cooperadoId));
    }
 
    @GetMapping("/abertas/por-cooperado/{cooperadoId}")
    @Operation(summary = "Listar triagens em aberto do cooperado — tela 2.1 e 2.3")
    public ResponseEntity<List<TriagemResponse>> listarAbertasPorCooperado(@PathVariable UUID cooperadoId) {
        return ResponseEntity.ok(service.listarAbertasPorCooperado(cooperadoId));
    }
 
    @PostMapping
    @Operation(summary = "Criar triagem — tela 4.8.2")
    public ResponseEntity<TriagemResponse> criar(@RequestBody @Valid TriagemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }
 
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar triagem")
    public ResponseEntity<TriagemResponse> atualizar(
            @PathVariable UUID id,
            @RequestBody @Valid TriagemRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }
 
    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status da triagem")
    public ResponseEntity<TriagemResponse> atualizarStatus(
            @PathVariable UUID id,
            @RequestBody @Valid AtualizarStatusTriagemRequest request) {
        return ResponseEntity.ok(service.atualizarStatus(id, request));
    }
 
    @PatchMapping("/{id}/concluir")
    @Operation(summary = "Concluir triagem — tela 2.3")
    public ResponseEntity<TriagemResponse> concluir(
            @PathVariable UUID id,
            @RequestBody @Valid ConcluirTriagemRequest request) {
        return ResponseEntity.ok(service.concluir(id, request));
    }
 
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar triagem")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}