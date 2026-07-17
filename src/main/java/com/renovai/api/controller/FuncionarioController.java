package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.PreCadastroRequest;
import com.renovai.api.dto.response.Responses.AvaliacaoResponse;
import com.renovai.api.service.FuncionarioService;
import com.renovai.api.service.FuncionarioService.FuncionarioRequest;
import com.renovai.api.service.FuncionarioService.FuncionarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/funcionarios")
@Tag(name = "Funcionários", description = "Gestão de cooperados e funcionários")
public class FuncionarioController {

    private final FuncionarioService service;

    public FuncionarioController(FuncionarioService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar funcionários", description = "Filtrável por ativos.")
    public ResponseEntity<List<FuncionarioResponse>> listar(
            @RequestParam(defaultValue = "false") boolean apenasAtivos) {
        if (apenasAtivos) return ResponseEntity.ok(service.listarAtivos());
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar funcionário por ID")
    public ResponseEntity<FuncionarioResponse> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_SITE','ADMIN_COOPERATIVA')")
    @Operation(summary = "Admitir funcionário/cooperado")
    public ResponseEntity<FuncionarioResponse> criar(@RequestBody FuncionarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }


    @GetMapping("/por-cargo/{cargo}")
    @Operation(summary = "Listar funcionários por cargo")
    public ResponseEntity<List<FuncionarioResponse>> listarPorCargo(@PathVariable String cargo) {
        return ResponseEntity.ok(service.listarPorCargo(cargo));
    }

    @PatchMapping("/{id}/cargo")
    @PreAuthorize("hasAnyRole('ADMIN_SITE','ADMIN_COOPERATIVA')")
    @Operation(summary = "Alterar cargo do funcionário")
    public ResponseEntity<FuncionarioResponse> atualizarCargo(
            @PathVariable Integer id, @RequestParam Integer cargoId) {
        return ResponseEntity.ok(service.atualizarCargo(id, cargoId));
    }


    @PostMapping("/pre-cadastro")
    @PreAuthorize("hasAnyRole('ADMIN_SITE','ADMIN_COOPERATIVA', 'GESTOR_COOPERATIVA')")
    @Operation(summary = "Realizar pré-cadastro de cooperado")
    public ResponseEntity<FuncionarioResponse> preCadastro(
            @RequestBody PreCadastroRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.preCadastro(request));
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasAnyRole('ADMIN_SITE','ADMIN_COOPERATIVA', 'GESTOR_COOPERATIVA')")
    @Operation(summary = "Desativar funcionário")
    public ResponseEntity<FuncionarioResponse> desativar(@PathVariable Integer id) {
        return ResponseEntity.ok(service.desativar(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SITE')")
    @Operation(summary = "Excluir funcionário permanentemente")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
