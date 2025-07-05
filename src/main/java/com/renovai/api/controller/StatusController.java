package com.renovai.api.controller;

import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.Status;
import com.renovai.api.repository.StatusRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/status")
@Tag(name = "Status", description = "Tabela de domínio — estados de coletas, triagens e pedidos")
public class StatusController {

    private final StatusRepository repository;

    public StatusController(StatusRepository repository) {
        this.repository = repository;
    }

    public record StatusRequest(@NotBlank String statusAtual) {}
    public record StatusResponse(Integer statusId, String statusAtual) {}

    @GetMapping
    @Operation(summary = "Listar todos os status")
    public ResponseEntity<List<StatusResponse>> listar() {
        return ResponseEntity.ok(
                repository.findAll().stream()
                        .map(s -> new StatusResponse(s.getStatusId(), s.getStatusAtual()))
                        .toList()
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar status por ID")
    public ResponseEntity<StatusResponse> buscarPorId(@PathVariable Integer id) {
        Status s = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Status", id));
        return ResponseEntity.ok(new StatusResponse(s.getStatusId(), s.getStatusAtual()));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_SITE')")
    @Operation(summary = "Criar novo status")
    public ResponseEntity<StatusResponse> criar(@RequestBody @Valid StatusRequest request) {
        Status s = new Status();
        s.setStatusAtual(request.statusAtual());
        Status saved = repository.save(s);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new StatusResponse(saved.getStatusId(), saved.getStatusAtual()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SITE')")
    @Operation(summary = "Atualizar status")
    public ResponseEntity<StatusResponse> atualizar(
            @PathVariable Integer id, @RequestBody @Valid StatusRequest request) {
        Status s = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Status", id));
        s.setStatusAtual(request.statusAtual());
        Status saved = repository.save(s);
        return ResponseEntity.ok(new StatusResponse(saved.getStatusId(), saved.getStatusAtual()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SITE')")
    @Operation(summary = "Excluir status")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Status", id));
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
