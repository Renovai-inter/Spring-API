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
import java.util.UUID;

@RestController
@RequestMapping("/status")
@Tag(name = "Status", description = "Tabela de domínio — status de pedidos e eventos")
public class StatusController {
    private final StatusRepository repository;
    public StatusController(StatusRepository repository) { this.repository = repository; }

    public record StatusRequest(@NotBlank String statusAtual, String dataAtualizacao) {}
    public record StatusResponse(UUID statusId, String statusAtual, String dataAtualizacao) {}

    @GetMapping
    public ResponseEntity<List<StatusResponse>> listar() {
        return ResponseEntity.ok(repository.findAll().stream()
                .map(s -> new StatusResponse(s.getStatusId(), s.getStatusAtual(), 
                        s.getDataAtualizacao().toString()))
                .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StatusResponse> buscarPorId(@PathVariable UUID id) {
        Status s = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Status", id));
        return ResponseEntity.ok(new StatusResponse(s.getStatusId(), s.getStatusAtual(),
                s.getDataAtualizacao().toString()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_SITE','ADMIN_COOPERATIVA')")
    public ResponseEntity<StatusResponse> criar(@RequestBody @Valid StatusRequest request) {
        Status status = new Status();
        status.setStatusAtual(request.statusAtual());
        Status saved = repository.save(status);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new StatusResponse(saved.getStatusId(), saved.getStatusAtual(),
                        saved.getDataAtualizacao().toString()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SITE','ADMIN_COOPERATIVA')")
    public ResponseEntity<StatusResponse> atualizar(
            @PathVariable UUID id, @RequestBody @Valid StatusRequest request) {
        Status s = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Status", id));
        s.setStatusAtual(request.statusAtual());
        Status saved = repository.save(s);
        return ResponseEntity.ok(new StatusResponse(saved.getStatusId(), saved.getStatusAtual(),
                saved.getDataAtualizacao().toString()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SITE')")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Status", id));
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}