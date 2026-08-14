package com.renovai.api.controller;
 
import com.renovai.api.dto.request.Requests.CooperativaRequest;
import com.renovai.api.dto.response.Responses.CooperativaPerfilPublicoResponse;
import com.renovai.api.dto.response.Responses.CooperativaResponse;
import com.renovai.api.service.CooperativaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
 
@RestController
@RequestMapping("/cooperativas")
@Tag(name = "Cooperativas", description = "Gestão de cooperativas de reciclagem")
public class CooperativaController {
 
    private final CooperativaService service;
 
    public CooperativaController(CooperativaService service) {
        this.service = service;
    }
 
    @GetMapping
    @Operation(summary = "Listar todas as cooperativas")
    public ResponseEntity<List<CooperativaResponse>> listar() {
        return ResponseEntity.ok(service.listarTodas());
    }
 
    @GetMapping("/{id}")
    @Operation(summary = "Buscar cooperativa por ID")
    public ResponseEntity<CooperativaResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }
 
    @GetMapping("/por-nome/{nome}")
    @Operation(summary = "Buscar cooperativas por nome")
    public ResponseEntity<List<CooperativaResponse>> buscarPorNome(@PathVariable String nome) {
        return ResponseEntity.ok(service.buscarPorNome(nome));
    }
 
    @GetMapping("/buscar")
    @Operation(summary = "Buscar cooperativas com filtros — tela 5.2")
    public ResponseEntity<List<CooperativaResponse>> buscarComFiltros(
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) BigDecimal quantidadeMin) {
        return ResponseEntity.ok(service.buscarComFiltros(categoriaId, cidade, quantidadeMin));
    }
 
    @GetMapping("/{id}/perfil-publico")
    @Operation(summary = "Perfil público da cooperativa — tela 5.2.1")
    public ResponseEntity<CooperativaPerfilPublicoResponse> buscarPerfilPublico(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPerfilPublico(id));
    }
 
    @PostMapping
    @Operation(summary = "Criar cooperativa")
    public ResponseEntity<CooperativaResponse> criar(@RequestBody @Valid CooperativaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }
 
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar cooperativa")
    public ResponseEntity<CooperativaResponse> atualizar(
            @PathVariable UUID id,
            @RequestBody @Valid CooperativaRequest request) {
        return ResponseEntity.ok(service.atualizar(id, request));
    }
 
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar cooperativa")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}