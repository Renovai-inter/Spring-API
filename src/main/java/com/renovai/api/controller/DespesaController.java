package com.renovai.api.controller;
 
import com.renovai.api.dto.request.Requests.DespesaRequest;
import com.renovai.api.dto.request.Requests.LancamentoDespesaRequest;
import com.renovai.api.dto.response.Responses.DespesaResponse;
import com.renovai.api.dto.response.Responses.LancamentoDespesaResponse;
import com.renovai.api.dto.response.Responses.TotalDespesasMesResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.Cooperativa;
import com.renovai.api.model.Despesa;
import com.renovai.api.model.LancamentoDespesa;
import com.renovai.api.repository.CooperativaRepository;
import com.renovai.api.repository.DespesaRepository;
import com.renovai.api.repository.LancamentoDespesaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
 
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
 
@RestController
@RequestMapping("/despesas")
@Tag(name = "Despesas", description = "Despesas fixas e variáveis da cooperativa — tela 4.7.1")
@Transactional
public class DespesaController {
 
    private final DespesaRepository repository;
    private final LancamentoDespesaRepository lancamentoRepository;
    private final CooperativaRepository cooperativaRepository;
 
    public DespesaController(DespesaRepository repository,
                              LancamentoDespesaRepository lancamentoRepository,
                              CooperativaRepository cooperativaRepository) {
        this.repository = repository;
        this.lancamentoRepository = lancamentoRepository;
        this.cooperativaRepository = cooperativaRepository;
    }
 
    @GetMapping("/por-cooperativa/{cooperativaId}")
    @Operation(summary = "Listar despesas da cooperativa")
    public ResponseEntity<List<DespesaResponse>> listarPorCooperativa(@PathVariable UUID cooperativaId) {
        return ResponseEntity.ok(repository.findByCooperativa_CooperativaId(cooperativaId)
                .stream().map(this::toResponse).toList());
    }
 
    @GetMapping("/ativas/por-cooperativa/{cooperativaId}")
    @Operation(summary = "Listar despesas ativas da cooperativa")
    public ResponseEntity<List<DespesaResponse>> listarAtivasPorCooperativa(
            @PathVariable UUID cooperativaId) {
        return ResponseEntity.ok(
                repository.findByCooperativa_CooperativaIdAndEstaAtivaTrue(cooperativaId)
                        .stream().map(this::toResponse).toList());
    }
 
    @GetMapping("/{id}")
    @Operation(summary = "Buscar despesa por ID")
    public ResponseEntity<DespesaResponse> buscarPorId(@PathVariable UUID id) {
        Despesa d = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Despesa", id));
        return ResponseEntity.ok(toResponse(d));
    }
 
    @PostMapping
    @Operation(summary = "Criar despesa")
    public ResponseEntity<DespesaResponse> criar(@RequestBody @Valid DespesaRequest request) {
        Cooperativa cooperativa = cooperativaRepository.findById(request.cooperativaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", request.cooperativaId()));
        Despesa despesa = new Despesa();
        despesa.setCooperativa(cooperativa);
        despesa.setNome(request.nome());
        despesa.setTipoDespesa(request.tipoDespesa());
        despesa.setEstaAtiva(request.estaAtiva() != null ? request.estaAtiva() : true);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(repository.save(despesa)));
    }
 
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar despesa")
    public ResponseEntity<DespesaResponse> atualizar(
            @PathVariable UUID id, @RequestBody @Valid DespesaRequest request) {
        Despesa d = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Despesa", id));
        d.setNome(request.nome());
        d.setTipoDespesa(request.tipoDespesa());
        if (request.estaAtiva() != null) d.setEstaAtiva(request.estaAtiva());
        return ResponseEntity.ok(toResponse(repository.save(d)));
    }
 
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar despesa")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Despesa", id));
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
 
    @GetMapping("/{despesaId}/lancamentos")
    @Operation(summary = "Listar lançamentos de uma despesa")
    public ResponseEntity<List<LancamentoDespesaResponse>> listarLancamentos(
            @PathVariable UUID despesaId) {
        return ResponseEntity.ok(lancamentoRepository.findByDespesa_DespesaId(despesaId)
                .stream().map(this::toLancamentoResponse).toList());
    }
 
    @GetMapping("/lancamentos/por-cooperativa/{cooperativaId}/mes/{mesReferencia}")
    @Operation(summary = "Listar lançamentos do mês por cooperativa — tela 4.7.1")
    public ResponseEntity<List<LancamentoDespesaResponse>> listarLancamentosPorMes(
            @PathVariable UUID cooperativaId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mesReferencia) {
        return ResponseEntity.ok(
                lancamentoRepository.findByCooperativaAndMes(cooperativaId, mesReferencia)
                        .stream().map(this::toLancamentoResponse).toList());
    }
 
    @GetMapping("/lancamentos/total/por-cooperativa/{cooperativaId}/mes/{mesReferencia}")
    @Operation(summary = "Total de despesas do mês — tela 4.7.1")
    public ResponseEntity<TotalDespesasMesResponse> totalDoMes(
            @PathVariable UUID cooperativaId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate mesReferencia) {
        BigDecimal totalFixas = lancamentoRepository.sumByCooperativaAndMesAndTipo(cooperativaId, mesReferencia, "FIXA");
        BigDecimal totalVariaveis = lancamentoRepository.sumByCooperativaAndMesAndTipo(cooperativaId, mesReferencia, "VARIAVEL");
        BigDecimal fixas = totalFixas != null ? totalFixas : BigDecimal.ZERO;
        BigDecimal variaveis = totalVariaveis != null ? totalVariaveis : BigDecimal.ZERO;
        return ResponseEntity.ok(new TotalDespesasMesResponse(mesReferencia, fixas, variaveis, fixas.add(variaveis)));
    }
 
    @PostMapping("/lancamentos")
    @Operation(summary = "Lançar despesa do mês — tela 4.7.1")
    public ResponseEntity<LancamentoDespesaResponse> lancar(
            @RequestBody @Valid LancamentoDespesaRequest request) {
        Despesa despesa = repository.findById(request.despesaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Despesa", request.despesaId()));
        LancamentoDespesa lancamento = new LancamentoDespesa();
        lancamento.setDespesa(despesa);
        lancamento.setValor(request.valor());
        lancamento.setMesReferencia(request.mesReferencia());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toLancamentoResponse(lancamentoRepository.save(lancamento)));
    }
 
    @DeleteMapping("/lancamentos/{id}")
    @Operation(summary = "Remover lançamento de despesa")
    public ResponseEntity<Void> deletarLancamento(@PathVariable UUID id) {
        lancamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("LancamentoDespesa", id));
        lancamentoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
 
    private DespesaResponse toResponse(Despesa d) {
        return new DespesaResponse(
                d.getDespesaId(),
                d.getCooperativa().getCooperativaId(),
                d.getCooperativa().getNome(),
                d.getNome(),
                d.getTipoDespesa(),
                d.getEstaAtiva()
        );
    }
 
    private LancamentoDespesaResponse toLancamentoResponse(LancamentoDespesa l) {
        return new LancamentoDespesaResponse(
                l.getLancamentoId(),
                l.getDespesa().getDespesaId(),
                l.getDespesa().getNome(),
                l.getDespesa().getTipoDespesa(),
                l.getValor(),
                l.getMesReferencia(),
                l.getDataLancamento()
        );
    }
}