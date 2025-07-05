package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.PerfilRequest;
import com.renovai.api.dto.response.Responses.PerfilResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.*;
import com.renovai.api.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/perfis")
@Tag(name = "Perfis", description = "Perfis de acesso de empresas e cooperativas")
public class PerfilController {

    private final PerfilRepository repository;
    private final EmpresaRepository empresaRepository;
    private final CooperativaRepository cooperativaRepository;
    private final EnderecoRepository enderecoRepository;

    public PerfilController(PerfilRepository repository,
                            EmpresaRepository empresaRepository,
                            CooperativaRepository cooperativaRepository,
                            EnderecoRepository enderecoRepository) {
        this.repository = repository;
        this.empresaRepository = empresaRepository;
        this.cooperativaRepository = cooperativaRepository;
        this.enderecoRepository = enderecoRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_SITE')")
    @Operation(summary = "Listar perfis ativos")
    public ResponseEntity<List<PerfilResponse>> listar() {
        return ResponseEntity.ok(
                repository.findByEstaAtivoTrue().stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar perfil por ID")
    public ResponseEntity<PerfilResponse> buscarPorId(@PathVariable Integer id) {
        Perfil p = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil", id));
        return ResponseEntity.ok(toResponse(p));
    }

    @PostMapping
    @Operation(summary = "Criar perfil",
               description = "Informe empresaId OU cooperativaId — nunca ambos.")
    public ResponseEntity<PerfilResponse> criar(@RequestBody @Valid PerfilRequest request) {
        validarTipo(request);
        if (repository.existsByEmail(request.email())) {
            throw new RegraDeNegocioException("Email já cadastrado no sistema.");
        }
        Perfil perfil = montarPerfil(new Perfil(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(repository.save(perfil)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SITE','ADMIN_COOPERATIVA')")
    @Operation(summary = "Atualizar perfil")
    public ResponseEntity<PerfilResponse> atualizar(
            @PathVariable Integer id, @RequestBody @Valid PerfilRequest request) {
        validarTipo(request);
        Perfil perfil = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil", id));
        montarPerfil(perfil, request);
        return ResponseEntity.ok(toResponse(repository.save(perfil)));
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize("hasAnyRole('ADMIN_SITE','ADMIN_COOPERATIVA')")
    @Operation(summary = "Desativar perfil")
    public ResponseEntity<PerfilResponse> desativar(@PathVariable Integer id) {
        Perfil perfil = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil", id));
        perfil.setEstaAtivo(false);
        return ResponseEntity.ok(toResponse(repository.save(perfil)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SITE')")
    @Operation(summary = "Excluir perfil")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Perfil", id));
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void validarTipo(PerfilRequest r) {
        boolean temEmpresa = r.empresaId() != null;
        boolean temCoop = r.cooperativaId() != null;
        if (temEmpresa == temCoop) {
            throw new RegraDeNegocioException(
                    "Informe exatamente um dos campos: empresaId ou cooperativaId.");
        }
    }

    private Perfil montarPerfil(Perfil perfil, PerfilRequest request) {
        perfil.setEmail(request.email());
        perfil.setCnpj(request.cnpj());

        if (request.empresaId() != null) {
            Empresa empresa = empresaRepository.findById(request.empresaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa", request.empresaId()));
            perfil.setEmpresa(empresa);
            perfil.setCooperativa(null);
        } else {
            Cooperativa coop = cooperativaRepository.findById(request.cooperativaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", request.cooperativaId()));
            perfil.setCooperativa(coop);
            perfil.setEmpresa(null);
        }

        if (request.enderecoId() != null) {
            Endereco end = enderecoRepository.findById(request.enderecoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Endereço", request.enderecoId()));
            perfil.setEndereco(end);
        }
        return perfil;
    }

    private PerfilResponse toResponse(Perfil p) {
        return new PerfilResponse(
                p.getPerfilId(), p.getEmail(), p.getCnpj(), p.getEstaAtivo(), p.getDataCriacao(),
                p.getEmpresa() != null ? p.getEmpresa().getEmpresaId() : null,
                p.getEmpresa() != null ? p.getEmpresa().getNome() : null,
                p.getCooperativa() != null ? p.getCooperativa().getCooperativaId() : null,
                p.getCooperativa() != null ? p.getCooperativa().getNome() : null
        );
    }
}
