package com.renovai.api.controller;
 
import com.renovai.api.dto.request.Requests.FavoritoRequest;
import com.renovai.api.dto.response.Responses.FavoritoResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.Cooperativa;
import com.renovai.api.model.Empresa;
import com.renovai.api.model.EmpresaCooperativaFavorita;
import com.renovai.api.repository.CooperativaRepository;
import com.renovai.api.repository.EmpresaCooperativaFavoritaRepository;
import com.renovai.api.repository.EmpresaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
import java.util.UUID;
 
@RestController
@RequestMapping("/favoritos")
@Tag(name = "Favoritos", description = "Cooperativas favoritas da empresa — telas 5.1 e 5.5")
public class FavoritoController {
 
    private final EmpresaCooperativaFavoritaRepository repository;
    private final EmpresaRepository empresaRepository;
    private final CooperativaRepository cooperativaRepository;
 
    public FavoritoController(EmpresaCooperativaFavoritaRepository repository,
                               EmpresaRepository empresaRepository,
                               CooperativaRepository cooperativaRepository) {
        this.repository = repository;
        this.empresaRepository = empresaRepository;
        this.cooperativaRepository = cooperativaRepository;
    }
 
    @GetMapping("/por-empresa/{empresaId}")
    @Operation(summary = "Listar cooperativas favoritas da empresa — tela 5.5")
    public ResponseEntity<List<FavoritoResponse>> listarPorEmpresa(@PathVariable UUID empresaId) {
        return ResponseEntity.ok(
                repository.findByEmpresa_EmpresaId(empresaId).stream()
                        .map(this::toResponse).toList()
        );
    }
 
    @PostMapping
    @Operation(summary = "Favoritar cooperativa — telas 5.2 e 5.2.1")
    public ResponseEntity<FavoritoResponse> favoritar(@RequestBody @Valid FavoritoRequest request) {
        if (repository.existsByEmpresa_EmpresaIdAndCooperativa_CooperativaId(
                request.empresaId(), request.cooperativaId())) {
            throw new RegraDeNegocioException("Cooperativa já está nos favoritos.");
        }
        Empresa empresa = empresaRepository.findById(request.empresaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa", request.empresaId()));
        Cooperativa cooperativa = cooperativaRepository.findById(request.cooperativaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", request.cooperativaId()));
        EmpresaCooperativaFavorita favorito = new EmpresaCooperativaFavorita();
        favorito.setEmpresa(empresa);
        favorito.setCooperativa(cooperativa);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(repository.save(favorito)));
    }
 
    @DeleteMapping("/{id}")
    @Operation(summary = "Desfavoritar cooperativa por ID do favorito")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Favorito", id));
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
 
    @DeleteMapping("/por-empresa-cooperativa")
    @Operation(summary = "Desfavoritar cooperativa — tela 5.5")
    public ResponseEntity<Void> desfavoritar(
            @RequestParam UUID empresaId,
            @RequestParam UUID cooperativaId) {
        EmpresaCooperativaFavorita f = repository
                .findByEmpresa_EmpresaIdAndCooperativa_CooperativaId(empresaId, cooperativaId)
                .orElseThrow(() -> new RegraDeNegocioException("Favorito não encontrado."));
        repository.deleteById(f.getFavoritoId());
        return ResponseEntity.noContent().build();
    }
 
    private FavoritoResponse toResponse(EmpresaCooperativaFavorita f) {
        return new FavoritoResponse(
                f.getFavoritoId(),
                f.getEmpresa().getEmpresaId(),
                f.getCooperativa().getCooperativaId(),
                f.getCooperativa().getNome(),
                f.getCooperativa().getImagemUrl(),
                f.getDataCriacao()
        );
    }
}