package com.renovai.api.controller;
 
import com.renovai.api.dto.request.Requests.RotaRequest;
import com.renovai.api.dto.request.Requests.RotaEnderecoRequest;
import com.renovai.api.dto.response.Responses.RotaEnderecoResponse;
import com.renovai.api.dto.response.Responses.RotaResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.Cooperativa;
import com.renovai.api.model.Endereco;
import com.renovai.api.model.Rota;
import com.renovai.api.model.RotaEndereco;
import com.renovai.api.repository.CooperativaRepository;
import com.renovai.api.repository.EnderecoRepository;
import com.renovai.api.repository.RotaEnderecoRepository;
import com.renovai.api.repository.RotaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
import java.util.UUID;
 
@RestController
@RequestMapping("/rotas")
@Tag(name = "Rotas", description = "Rotas de coleta dos motoristas — telas 3.1 e 3.2")
@Transactional
public class RotaController {
 
    private final RotaRepository repository;
    private final RotaEnderecoRepository rotaEnderecoRepository;
    private final CooperativaRepository cooperativaRepository;
    private final EnderecoRepository enderecoRepository;
 
    public RotaController(RotaRepository repository,
                          RotaEnderecoRepository rotaEnderecoRepository,
                          CooperativaRepository cooperativaRepository,
                          EnderecoRepository enderecoRepository) {
        this.repository = repository;
        this.rotaEnderecoRepository = rotaEnderecoRepository;
        this.cooperativaRepository = cooperativaRepository;
        this.enderecoRepository = enderecoRepository;
    }
 
    @GetMapping("/por-cooperativa/{cooperativaId}")
    @Operation(summary = "Listar rotas da cooperativa")
    public ResponseEntity<List<RotaResponse>> listarPorCooperativa(@PathVariable UUID cooperativaId) {
        return ResponseEntity.ok(repository.findByCooperativa_CooperativaId(cooperativaId)
                .stream().map(this::toResponse).toList());
    }
 
    @GetMapping("/ativas/por-cooperativa/{cooperativaId}")
    @Operation(summary = "Listar rotas ativas da cooperativa — tela 3.1")
    public ResponseEntity<List<RotaResponse>> listarAtivasPorCooperativa(@PathVariable UUID cooperativaId) {
        return ResponseEntity.ok(
                repository.findByCooperativa_CooperativaIdAndEstaAtivaTrue(cooperativaId)
                        .stream().map(this::toResponse).toList());
    }
 
    @GetMapping("/{id}")
    @Operation(summary = "Buscar rota por ID — tela 3.2")
    public ResponseEntity<RotaResponse> buscarPorId(@PathVariable UUID id) {
        Rota rota = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Rota", id));
        return ResponseEntity.ok(toResponse(rota));
    }
 
    @PostMapping
    @Operation(summary = "Criar rota")
    public ResponseEntity<RotaResponse> criar(@RequestBody @Valid RotaRequest request) {
        Cooperativa cooperativa = cooperativaRepository.findById(request.cooperativaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", request.cooperativaId()));
        if (repository.existsByNomeAndCooperativa_CooperativaId(request.nome(), request.cooperativaId())) {
            throw new RegraDeNegocioException("Já existe rota com esse nome nessa cooperativa.");
        }
        Rota rota = new Rota();
        rota.setCooperativa(cooperativa);
        rota.setNome(request.nome());
        rota.setEstaAtiva(request.estaAtiva() != null ? request.estaAtiva() : true);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(repository.save(rota)));
    }
 
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar rota")
    public ResponseEntity<RotaResponse> atualizar(
            @PathVariable UUID id, @RequestBody @Valid RotaRequest request) {
        Rota rota = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Rota", id));
        rota.setNome(request.nome());
        if (request.estaAtiva() != null) rota.setEstaAtiva(request.estaAtiva());
        return ResponseEntity.ok(toResponse(repository.save(rota)));
    }
 
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar rota")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Rota", id));
        rotaEnderecoRepository.deleteByRota_RotaId(id);
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
 
    @GetMapping("/{rotaId}/enderecos")
    @Operation(summary = "Listar endereços da rota em ordem — tela 3.2")
    public ResponseEntity<List<RotaEnderecoResponse>> listarEnderecos(@PathVariable UUID rotaId) {
        return ResponseEntity.ok(rotaEnderecoRepository.findByRota_RotaIdOrderByOrdemAsc(rotaId)
                .stream().map(this::toEnderecoResponse).toList());
    }
 
    @PostMapping("/enderecos")
    @Operation(summary = "Adicionar endereço à rota")
    public ResponseEntity<RotaEnderecoResponse> adicionarEndereco(
            @RequestBody @Valid RotaEnderecoRequest request) {
        Rota rota = repository.findById(request.rotaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Rota", request.rotaId()));
        Endereco endereco = enderecoRepository.findById(request.enderecoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Endereco", request.enderecoId()));
        RotaEndereco re = new RotaEndereco();
        re.setRota(rota);
        re.setEndereco(endereco);
        re.setNomeLocal(request.nomeLocal());
        re.setTipoLocal(request.tipoLocal());
        re.setOrdem(request.ordem());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toEnderecoResponse(rotaEnderecoRepository.save(re)));
    }
 
    @DeleteMapping("/enderecos/{id}")
    @Operation(summary = "Remover endereço da rota")
    public ResponseEntity<Void> removerEndereco(@PathVariable UUID id) {
        rotaEnderecoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("RotaEndereco", id));
        rotaEnderecoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
 
    private RotaResponse toResponse(Rota r) {
        List<RotaEnderecoResponse> enderecos = rotaEnderecoRepository
                .findByRota_RotaIdOrderByOrdemAsc(r.getRotaId())
                .stream().map(this::toEnderecoResponse).toList();
        return new RotaResponse(
                r.getRotaId(),
                r.getCooperativa().getCooperativaId(),
                r.getCooperativa().getNome(),
                r.getNome(),
                r.getEstaAtiva(),
                enderecos
        );
    }
 
    private RotaEnderecoResponse toEnderecoResponse(RotaEndereco re) {
        Endereco e = re.getEndereco();
        return new RotaEnderecoResponse(
                re.getRotaEnderecoId(),
                re.getRota().getRotaId(),
                e.getEnderecoId(),
                re.getNomeLocal(),
                re.getTipoLocal(),
                re.getOrdem(),
                e.getLogradouro(),
                e.getNumero(),
                e.getBairro(),
                e.getCidade()
        );
    }
}