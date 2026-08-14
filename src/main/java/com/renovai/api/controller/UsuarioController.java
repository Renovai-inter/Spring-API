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
 
    public UsuarioController(UsuarioService service) {
        this.service = service;
    }
 
    @GetMapping
    @Operation(summary = "Listar todos os usuários")
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }
 
    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }
 
    @PostMapping
    @Operation(summary = "Criar usuário")
    public ResponseEntity<UsuarioResponse> criar(@RequestBody @Valid UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }
 
    @PostMapping("/validar-primeiro-acesso")
    @Operation(summary = "Verificar pré-cadastro — tela 1.5")
    public ResponseEntity<PrimeiroAcessoResponse> validarPrimeiroAcesso(
            @RequestBody @Valid ValidarPrimeiroAcessoRequest request) {
        return ResponseEntity.ok(service.validarPrimeiroAcesso(request));
    }
 
    @PutMapping("/{id}/completar-cadastro")
    @Operation(summary = "Completar cadastro após pré-cadastro — tela 1.6")
    public ResponseEntity<UsuarioResponse> completarCadastro(
            @PathVariable UUID id,
            @RequestBody @Valid CompletarCadastroRequest request) {
        return ResponseEntity.ok(service.completarCadastro(request));
    }
 
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar dados do usuário")
    public ResponseEntity<UsuarioResponse> atualizar(
            @PathVariable UUID id,
            @RequestBody @Valid UsuarioRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }
 
    @PutMapping("/{id}/alterar-senha")
    @Operation(summary = "Alterar senha do usuário")
    public ResponseEntity<Void> alterarSenha(
            @PathVariable UUID id,
            @RequestBody @Valid AlterarSenhaRequest request) {
        service.alterarSenha(id, request);
        return ResponseEntity.noContent().build();
    }
 
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar usuário")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}