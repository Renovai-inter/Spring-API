package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.UsuarioRequest;
import com.renovai.api.dto.request.Requests.CompletarCadastroRequest;
import com.renovai.api.dto.request.Requests.ValidarPrimeiroAcessoRequest;
import com.renovai.api.dto.request.Requests.AlterarSenhaRequest;
import com.renovai.api.dto.response.Responses.UsuarioResponse;
import com.renovai.api.dto.response.Responses.PrimeiroAcessoResponse;
import com.renovai.api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuários", description = "Gestão de usuários da plataforma")
public class UsuarioController {
    private final UsuarioService service;
    public UsuarioController(UsuarioService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@RequestBody @Valid UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @PutMapping("/{id}/completar-cadastro")
    public ResponseEntity<UsuarioResponse> completarCadastro(
            @PathVariable UUID id, @RequestBody @Valid CompletarCadastroRequest request) {
        return ResponseEntity.ok(service.completarCadastro(request));
    }

    @PostMapping("/validar-primeiro-acesso")
    public ResponseEntity<PrimeiroAcessoResponse> validarPrimeiroAcesso(
            @RequestBody @Valid ValidarPrimeiroAcessoRequest request) {
        return ResponseEntity.ok(service.validarPrimeiroAcesso(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(
            @PathVariable UUID id, @RequestBody @Valid UsuarioRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/alterar-senha")
    public ResponseEntity<Void> alterarSenha(
            @PathVariable UUID id, @RequestBody @Valid AlterarSenhaRequest request) {
        service.alterarSenha(id, request);
        return ResponseEntity.noContent().build();
    }
}