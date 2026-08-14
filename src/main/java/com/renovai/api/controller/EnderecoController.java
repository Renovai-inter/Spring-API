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
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
import java.util.UUID;
 
@RestController
@RequestMapping("/enderecos")
@Tag(name = "Endereços", description = "Gestão de endereços")
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
    public ResponseEntity<EnderecoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(findOrThrow(id)));
    }
 
    @PostMapping
    @Operation(summary = "Criar endereço")
    public ResponseEntity<EnderecoResponse> criar(@RequestBody @Valid EnderecoRequest request) {
        Endereco endereco = new Endereco();
        endereco.setCep(request.cep());
        endereco.setLogradouro(request.logradouro());
        endereco.setNumero(request.numero());
        endereco.setComplemento(request.complemento());
        endereco.setBairro(request.bairro());
        endereco.setCidade(request.cidade());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(repository.save(endereco)));
    }
 
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar endereço")
    public ResponseEntity<EnderecoResponse> atualizar(
            @PathVariable UUID id, @RequestBody @Valid EnderecoRequest request) {
        Endereco e = findOrThrow(id);
        e.setCep(request.cep());
        e.setLogradouro(request.logradouro());
        e.setNumero(request.numero());
        e.setComplemento(request.complemento());
        e.setBairro(request.bairro());
        e.setCidade(request.cidade());
        return ResponseEntity.ok(toResponse(repository.save(e)));
    }
 
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar endereço")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        findOrThrow(id);
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
 
    private Endereco findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereco", id));
    }
 
    private EnderecoResponse toResponse(Endereco e) {
        return new EnderecoResponse(e.getEnderecoId(), e.getCep(), e.getLogradouro(),
                e.getNumero(), e.getComplemento(), e.getBairro(), e.getCidade(), null);
    }
}