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
    @Operation(summary = "Listar funcionários por cooperativa — tela 4.9")
    public ResponseEntity<List<FuncionarioResponse>> listarPorCooperativa(
            @PathVariable UUID cooperativaId) {
        return ResponseEntity.ok(service.listarPorCooperativa(cooperativaId));
    }
 
    @GetMapping("/motoristas/por-cooperativa/{cooperativaId}")
    @Operation(summary = "Listar motoristas da cooperativa — tela 3.1")
    public ResponseEntity<List<FuncionarioResponse>> listarMotoristasPorCooperativa(
            @PathVariable UUID cooperativaId) {
        return ResponseEntity.ok(service.listarMotoristasPorCooperativa(cooperativaId));
    }
 
    @GetMapping("/por-cooperativa/{cooperativaId}/por-status/{status}")
    @Operation(summary = "Listar funcionários por cooperativa e status")
    public ResponseEntity<List<FuncionarioResponse>> listarPorCooperativaEStatus(
            @PathVariable UUID cooperativaId,
            @PathVariable String status) {
        return ResponseEntity.ok(service.listarPorCooperativaEStatus(cooperativaId, status));
    }
 
    @GetMapping("/pre-cadastro/incompletos")
    @Operation(summary = "Listar pré-cadastros incompletos")
    public ResponseEntity<List<PreCadastroIncompletoResponse>> listarComPreCadastroIncompleto() {
        return ResponseEntity.ok(service.listarComPreCadastroIncompleto());
    }
 
    @GetMapping("/pre-cadastro/incompletos/por-cooperativa/{cooperativaId}")
    @Operation(summary = "Listar pré-cadastros incompletos por cooperativa")
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
    @Operation(summary = "Criar funcionário")
    public ResponseEntity<FuncionarioResponse> criar(@RequestBody @Valid FuncionarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }
 
    @PostMapping("/pre-cadastro")
    @Operation(summary = "Pré-cadastro de cooperado — tela 1.5")
    public ResponseEntity<FuncionarioResponse> preCadastro(@RequestBody @Valid PreCadastroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.preCadastro(request));
    }
 
    @PutMapping("/{id}/cargo/{cargoId}")
    @Operation(summary = "Atualizar cargo do funcionário")
    public ResponseEntity<FuncionarioResponse> atualizarCargo(
            @PathVariable UUID id, @PathVariable UUID cargoId) {
        return ResponseEntity.ok(service.atualizarCargo(id, cargoId));
    }
 
    @PutMapping("/{id}/desativar")
    @Operation(summary = "Desativar funcionário")
    public ResponseEntity<FuncionarioResponse> desativar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.desativar(id));
    }
 
    @PutMapping("/{id}/afastar")
    @Operation(summary = "Afastar funcionário")
    public ResponseEntity<FuncionarioResponse> afastar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.afastar(id));
    }
 
    @PutMapping("/{id}/reativar")
    @Operation(summary = "Reativar funcionário")
    public ResponseEntity<FuncionarioResponse> reativar(@PathVariable UUID id) {
        return ResponseEntity.ok(service.reativar(id));
    }
 
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar funcionário")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}