package com.renovai.api.service;
 
import com.renovai.api.dto.request.Requests.DespesaRequest;
import com.renovai.api.dto.request.Requests.LancamentoDespesaRequest;
import com.renovai.api.dto.response.Responses.DespesaResponse;
import com.renovai.api.dto.response.Responses.LancamentoDespesaResponse;
import com.renovai.api.dto.response.Responses.TotalDespesasMesResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.Cooperativa;
import com.renovai.api.model.Despesa;
import com.renovai.api.model.LancamentoDespesa;
import com.renovai.api.repository.CooperativaRepository;
import com.renovai.api.repository.DespesaRepository;
import com.renovai.api.repository.LancamentoDespesaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
 
@Service
@Transactional
public class DespesaService {
 
    private final DespesaRepository repository;
    private final LancamentoDespesaRepository lancamentoRepository;
    private final CooperativaRepository cooperativaRepository;
 
    public DespesaService(DespesaRepository repository,
                          LancamentoDespesaRepository lancamentoRepository,
                          CooperativaRepository cooperativaRepository) {
        this.repository = repository;
        this.lancamentoRepository = lancamentoRepository;
        this.cooperativaRepository = cooperativaRepository;
    }
 
    // ── Despesas ─────────────────────────────────────────────
 
    @Transactional(readOnly = true)
    public List<DespesaResponse> listarPorCooperativa(UUID cooperativaId) {
        return repository.findByCooperativa_CooperativaId(cooperativaId)
                .stream().map(this::toDespesaResponse).toList();
    }
 
    @Transactional(readOnly = true)
    public List<DespesaResponse> listarAtivasPorCooperativa(UUID cooperativaId) {
        return repository.findByCooperativa_CooperativaIdAndEstaAtivaTrue(cooperativaId)
                .stream().map(this::toDespesaResponse).toList();
    }
 
    @Transactional(readOnly = true)
    public DespesaResponse buscarPorId(UUID id) {
        return toDespesaResponse(findDespesaOrThrow(id));
    }
 
    public DespesaResponse criar(DespesaRequest request) {
        Cooperativa cooperativa = cooperativaRepository.findById(request.cooperativaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", request.cooperativaId()));
 
        Despesa despesa = Despesa.builder()
                .cooperativa(cooperativa)
                .nome(request.nome())
                .tipoDespesa(request.tipoDespesa())
                .estaAtiva(request.estaAtiva() != null ? request.estaAtiva() : true)
                .build();
        return toDespesaResponse(repository.save(despesa));
    }
 
    public DespesaResponse atualizar(UUID id, DespesaRequest request) {
        Despesa despesa = findDespesaOrThrow(id);
        despesa.setNome(request.nome());
        despesa.setTipoDespesa(request.tipoDespesa());
        if (request.estaAtiva() != null) despesa.setEstaAtiva(request.estaAtiva());
        return toDespesaResponse(repository.save(despesa));
    }
 
    public void deletar(UUID id) {
        findDespesaOrThrow(id);
        repository.deleteById(id);
    }
 
    // ── Lançamentos ──────────────────────────────────────────
 
    @Transactional(readOnly = true)
    public List<LancamentoDespesaResponse> listarLancamentosPorDespesa(UUID despesaId) {
        return lancamentoRepository.findByDespesa_DespesaId(despesaId)
                .stream().map(this::toLancamentoResponse).toList();
    }
 
    @Transactional(readOnly = true)
    public List<LancamentoDespesaResponse> listarLancamentosPorCooperativaEMes(
            UUID cooperativaId, LocalDate mesReferencia) {
        return lancamentoRepository.findByCooperativaAndMes(cooperativaId, mesReferencia)
                .stream().map(this::toLancamentoResponse).toList();
    }
 
    @Transactional(readOnly = true)
    public TotalDespesasMesResponse totalPorMes(UUID cooperativaId, LocalDate mesReferencia) {
        BigDecimal fixas = lancamentoRepository
                .sumByCooperativaAndMesAndTipo(cooperativaId, mesReferencia, "FIXA");
        BigDecimal variaveis = lancamentoRepository
                .sumByCooperativaAndMesAndTipo(cooperativaId, mesReferencia, "VARIAVEL");
        BigDecimal totalFixas = fixas != null ? fixas : BigDecimal.ZERO;
        BigDecimal totalVariaveis = variaveis != null ? variaveis : BigDecimal.ZERO;
        return new TotalDespesasMesResponse(
                mesReferencia, totalFixas, totalVariaveis, totalFixas.add(totalVariaveis));
    }
 
    public LancamentoDespesaResponse lancar(LancamentoDespesaRequest request) {
        Despesa despesa = findDespesaOrThrow(request.despesaId());
 
        boolean jaExiste = lancamentoRepository
                .findByDespesa_DespesaId(request.despesaId()).stream()
                .anyMatch(l -> l.getMesReferencia().equals(request.mesReferencia()));
        if (jaExiste) {
            throw new RegraDeNegocioException(
                    "Já existe lançamento para essa despesa no mês informado.");
        }
 
        LancamentoDespesa lancamento = LancamentoDespesa.builder()
                .despesa(despesa)
                .valor(request.valor())
                .mesReferencia(request.mesReferencia())
                .build();
        return toLancamentoResponse(lancamentoRepository.save(lancamento));
    }
 
    public void deletarLancamento(UUID id) {
        lancamentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("LancamentoDespesa", id));
        lancamentoRepository.deleteById(id);
    }
 
    // ── Helpers ──────────────────────────────────────────────
 
    private Despesa findDespesaOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Despesa", id));
    }
 
    private DespesaResponse toDespesaResponse(Despesa d) {
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