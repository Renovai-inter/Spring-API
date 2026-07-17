package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.AlterarSenhaRequest;
import com.renovai.api.dto.request.Requests.CompletarCadastroRequest;
import com.renovai.api.dto.request.Requests.UsuarioRequest;
import com.renovai.api.dto.request.Requests.ValidarPrimeiroAcessoRequest;
import com.renovai.api.dto.response.Responses.PrimeiroAcessoResponse;
import com.renovai.api.dto.response.Responses.UsuarioResponse;
import com.renovai.api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/usuarios")
@Tag(name = "Usuários", description = "Gestão de usuários do sistema")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_SITE', 'ADMIN_COOPERATIVA')")
    @Operation(summary = "Listar todos os usuários")
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar usuário por ID")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Cadastrar novo usuário")
    public ResponseEntity<UsuarioResponse> criar(@RequestBody @Valid UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar usuário")
    public ResponseEntity<UsuarioResponse> atualizar(
            @PathVariable Integer id, @RequestBody @Valid UsuarioRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SITE')")
    @Operation(summary = "Excluir usuário")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    

    @PostMapping("/primeiro-acesso")
    @Operation(summary = "Validar primeiro acesso")
    public ResponseEntity<PrimeiroAcessoResponse> primeiroAcesso(
            @RequestBody ValidarPrimeiroAcessoRequest request){

        return ResponseEntity.ok(
                service.validarPrimeiroAcesso(request)
        );
    }
    @PatchMapping("/completar-cadastro")
    @Operation(summary = "Completar cadastro")
    public ResponseEntity<UsuarioResponse> completarCadastro(
            @RequestBody CompletarCadastroRequest request){

        return ResponseEntity.ok(
                service.completarCadastro(request)
        );
    }

    @PutMapping("/{id}/senha")
    @Operation(summary = "Alterar senha do usuário")
    public ResponseEntity<Void> alterarSenha(@PathVariable Integer id, @RequestBody AlterarSenhaRequest request) {
        service.alterarSenha(id, request);
        return ResponseEntity.noContent().build();
    }
}
