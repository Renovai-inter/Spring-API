package com.renovai.api.controller;

import com.renovai.api.service.FuncionarioService;
import com.renovai.api.service.FuncionarioService.FuncionarioRequest;
import com.renovai.api.service.FuncionarioService.FuncionarioResponse;
import com.renovai.api.service.FuncionarioService.PreCadastroIncompletoResponse;
import com.renovai.api.dto.request.Requests.PreCadastroRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/funcionarios")
@Tag(name = "Funcionários", description = "Gestão de funcionários e cooperados")
public class FuncionarioController {
private final FuncionarioService service;
    
    public FuncionarioController(FuncionarioService service) { 
        this.service = service; 
    }
 
    @GetMapping
    @Operation(summary = "Listar todos os funcionários")
    public ResponseEntity<List<FuncionarioResponse>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }
 
    @GetMapping("/ativos")
    @Operation(summary = "Listar funcionários ativos")
    public ResponseEntity<List<FuncionarioResponse>> listarAtivos() {
        return ResponseEntity.ok(service.listarAtivos());
    }
 
    @GetMapping("/por-cargo/{cargo}")
    @Operation(summary = "Listar funcionários por cargo")
    public ResponseEntity<List<FuncionarioResponse>> listarPorCargo(@PathVariable String cargo) {
        return ResponseEntity.ok(service.listarPorCargo(cargo));
    }
 
    @GetMapping("/por-cooperativa/{cooperativaId}")
    @Operation(summary = "Listar funcionários por cooperativa")
    public ResponseEntity<List<FuncionarioResponse>> listarPorCooperativa(
            @PathVariable UUID cooperativaId) {
        return ResponseEntity.ok(service.listarPorCooperativa(cooperativaId));
    }

    @GetMapping("/pre-cadastro/incompletos")
    @Operation(summary = "Listar funcionários com pré-cadastro incompleto",
               description = "Retorna usuários que fizeram pré-cadastro (0.3) mas não completaram o cadastro complementar (0.4). " +
                             "Identifica-os pela ausência de email preenchido. Útil para o Admin acompanhar pendências.")
    public ResponseEntity<List<PreCadastroIncompletoResponse>> listarComPreCadastroIncompleto() {
        return ResponseEntity.ok(service.listarComPreCadastroIncompleto());
    }

    @GetMapping("/pre-cadastro/incompletos/por-cooperativa/{cooperativaId}")
    @Operation(summary = "Listar pré-cadastros incompletos por cooperativa",
               description = "Retorna funcionários de uma cooperativa específica que fizeram pré-cadastro mas não completaram. " +
                             "Útil para o Admin gerenciar pendências por unidade.")
    public ResponseEntity<List<PreCadastroIncompletoResponse>> listarComPreCadastroIncompletoByCooperativa(
            @PathVariable UUID cooperativaId) {
        return ResponseEntity.ok(service.listarComPreCadastroIncompletoByCooperativa(cooperativaId));
    }
 
    @GetMapping("/{id}")
    @Operation(summary = "Buscar funcionário por ID")
    public ResponseEntity<FuncionarioResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }
 
    @PostMapping
    @Operation(summary = "Criar novo funcionário")
    public ResponseEntity<FuncionarioResponse> criar(@RequestBody @Valid FuncionarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }
 
    @PutMapping("/{id}/cargo/{cargoId}")
    @Operation(summary = "Atualizar cargo do funcionário")
    public ResponseEntity<FuncionarioResponse> atualizarCargo(
            @PathVariable UUID id, @PathVariable UUID cargoId) {
        return ResponseEntity.ok(service.atualizarCargo(id, cargoId));
    }

    @PostMapping("/pre-cadastro")
    @Operation(summary = "Realizar pré-cadastro de funcionário",
               description = "Tela 0.3 do fluxo Renovaí. Cria usuário com CPF e senha temporária. " +
                             "Email será preenchido no cadastro complementar (0.4).")
    public ResponseEntity<FuncionarioResponse> preCadastro(
            @RequestBody @Valid PreCadastroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.preCadastro(request));
    }
 
    @PutMapping("/{id}/desativar")
    @Operation(summary = "Desativar funcionário")
    public ResponseEntity<FuncionarioResponse> desativar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.desativar(id));
    }
 
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar funcionário")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}