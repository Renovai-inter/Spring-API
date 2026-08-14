package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.EmpresaRequest;
import com.renovai.api.dto.response.Responses.EmpresaDashboardResponse;
import com.renovai.api.dto.response.Responses.EmpresaResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.Empresa;
import com.renovai.api.repository.EmpresaCooperativaFavoritaRepository;
import com.renovai.api.repository.EmpresaRepository;
import com.renovai.api.repository.NegociacaoRepository;
import com.renovai.api.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class EmpresaService {

    private final EmpresaRepository repository;
    private final NegociacaoRepository negociacaoRepository;
    private final EmpresaCooperativaFavoritaRepository favoritaRepository;
    private final PedidoRepository pedidoRepository;

    public EmpresaService(EmpresaRepository repository,
                          NegociacaoRepository negociacaoRepository,
                          EmpresaCooperativaFavoritaRepository favoritaRepository,
                          PedidoRepository pedidoRepository) {
        this.repository = repository;
        this.negociacaoRepository = negociacaoRepository;
        this.favoritaRepository = favoritaRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public List<EmpresaResponse> listarTodas() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EmpresaResponse buscarPorId(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<EmpresaResponse> buscarPorNome(String nome) {
        return repository.findByNomeContainingIgnoreCase(nome).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EmpresaDashboardResponse buscarDashboard(UUID empresaId) {
        findOrThrow(empresaId);
        long totalEnviados = pedidoRepository.findByEmpresa_EmpresaId(empresaId).size();
        Long totalAceitos = negociacaoRepository.countAceitosByEmpresa(empresaId);
        BigDecimal valorTotal = negociacaoRepository.sumValorNegociadoByEmpresa(empresaId);
        long totalFavoritas = favoritaRepository.countByEmpresa_EmpresaId(empresaId);

        return new EmpresaDashboardResponse(
                totalEnviados,
                totalAceitos != null ? totalAceitos : 0L,
                valorTotal != null ? valorTotal : BigDecimal.ZERO,
                totalFavoritas
        );
    }

    public EmpresaResponse criar(EmpresaRequest request) {
        Empresa empresa = Empresa.builder()
                .nome(request.nome())
                .descricao(request.descricao())
                .build();
        return toResponse(repository.save(empresa));
    }

    public EmpresaResponse atualizar(UUID id, EmpresaRequest request) {
        Empresa empresa = findOrThrow(id);
        empresa.setNome(request.nome());
        empresa.setDescricao(request.descricao());
        return toResponse(repository.save(empresa));
    }

    public void deletar(UUID id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    public Empresa findEntityById(UUID id) {
        return findOrThrow(id);
    }

    private Empresa findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa", id));
    }

    private EmpresaResponse toResponse(Empresa e) {
        return new EmpresaResponse(
                e.getEmpresaId(),
                e.getNome(),
                e.getDescricao(),
                null,
                null,
                null
        );
    }
}