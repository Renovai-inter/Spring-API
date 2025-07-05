package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.EnderecoRequest;
import com.renovai.api.dto.response.Responses.EnderecoResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.Endereco;
import com.renovai.api.repository.EnderecoRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/enderecos")
@Tag(name = "Endereços", description = "Cadastro de endereços de perfis")
public class EnderecoController {

    private final EnderecoRepository repository;

    public EnderecoController(EnderecoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    @Operation(summary = "Listar todos os endereços")
    public ResponseEntity<List<EnderecoResponse>> listar() {
        return ResponseEntity.ok(repository.findAll().stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar endereço por ID")
    public ResponseEntity<EnderecoResponse> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(toResponse(findOrThrow(id)));
    }

    @PostMapping
    @Operation(summary = "Cadastrar endereço")
    public ResponseEntity<EnderecoResponse> criar(@RequestBody @Valid EnderecoRequest request) {
        Endereco e = Endereco.builder()
                .cep(request.cep()).logradouro(request.logradouro()).numero(request.numero())
                .complemento(request.complemento()).bairro(request.bairro()).cidade(request.cidade())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(repository.save(e)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar endereço")
    public ResponseEntity<EnderecoResponse> atualizar(
            @PathVariable Integer id, @RequestBody @Valid EnderecoRequest request) {
        Endereco e = findOrThrow(id);
        e.setCep(request.cep()); e.setLogradouro(request.logradouro());
        e.setNumero(request.numero()); e.setComplemento(request.complemento());
        e.setBairro(request.bairro()); e.setCidade(request.cidade());
        return ResponseEntity.ok(toResponse(repository.save(e)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SITE')")
    @Operation(summary = "Excluir endereço")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        findOrThrow(id);
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private Endereco findOrThrow(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereço", id));
    }

    private EnderecoResponse toResponse(Endereco e) {
        return new EnderecoResponse(e.getEnderecoId(), e.getCep(), e.getLogradouro(),
                e.getNumero(), e.getComplemento(), e.getBairro(), e.getCidade());
    }
}
