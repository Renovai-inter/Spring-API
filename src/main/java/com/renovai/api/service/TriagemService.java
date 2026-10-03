package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.AtualizarStatusTriagemRequest;
import com.renovai.api.dto.request.Requests.ConcluirTriagemRequest;
import com.renovai.api.dto.request.Requests.TriagemRequest;
import com.renovai.api.dto.response.Responses.TriagemResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.*;
import com.renovai.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TriagemService {

    private final TriagemRepository repository;
    private final EquipeRepository equipeRepository;
    private final ColetaRepository coletaRepository;
    private final MaterialRepository materialRepository;
    private final StatusRepository statusRepository;
    private final EquipeCooperadoRepository equipeCooperadoRepository;
    private final TriagemEstoqueService estoque;

    public TriagemService(TriagemRepository repository,
                          EquipeRepository equipeRepository,
                          ColetaRepository coletaRepository,
                          MaterialRepository materialRepository,
                          StatusRepository statusRepository,
                          EquipeCooperadoRepository equipeCooperadoRepository,
                          TriagemEstoqueService estoque) {
        this.repository = repository;
        this.equipeRepository = equipeRepository;
        this.coletaRepository = coletaRepository;
        this.materialRepository = materialRepository;
        this.statusRepository = statusRepository;
        this.equipeCooperadoRepository = equipeCooperadoRepository;
        this.estoque = estoque;
    }

    @Transactional(readOnly = true)
    public List<TriagemResponse> listarTodas() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TriagemResponse buscarPorId(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<TriagemResponse> listarPorColeta(UUID coletaId) {
        return repository.findByColeta_EventoId(coletaId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TriagemResponse> listarPorCooperativa(UUID cooperativaId) {
        return repository.findByCooperativa(cooperativaId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TriagemResponse> listarPorCooperativaEStatus(UUID cooperativaId, String status) {
        return repository.findByCooperativaAndStatus(cooperativaId, status).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TriagemResponse> listarPorCooperado(UUID cooperadoId) {
        return repository.findByCooperado(cooperadoId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TriagemResponse> listarAbertasPorCooperado(UUID cooperadoId) {
        return repository.findAbertysByCooperado(cooperadoId).stream().map(this::toResponse).toList();
    }

    public TriagemResponse criar(TriagemRequest request) {
        estoque.bloquearColeta(request.coletaId());
        Equipe equipe = equipeRepository.findById(request.equipeId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipe", request.equipeId()));
        Coleta coleta = coletaRepository.findById(request.coletaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Coleta", request.coletaId()));
        Material material = materialRepository.findById(request.materialId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material", request.materialId()));
        UUID cooperativaId = coleta.getCooperado().getCooperativa().getCooperativaId();
        if (!cooperativaId.equals(equipe.getGestor().getCooperativa().getCooperativaId())
                || (material.getCooperativa() != null && !cooperativaId.equals(material.getCooperativa().getCooperativaId())))
            throw new RegraDeNegocioException("Equipe e material devem pertencer à cooperativa da coleta.");

        Status status = null;
        if (request.statusId() != null) {
            status = statusRepository.findById(request.statusId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Status", request.statusId()));
        }

        BigDecimal quantidadeRejeitoKg = request.quantidadeRejeitoKg() != null
                ? request.quantidadeRejeitoKg() : BigDecimal.ZERO;

        validarQuantidades(request.quantidadeKg(), quantidadeRejeitoKg);
        validarStatus(status);

        Triagem triagem = new Triagem();
        triagem.setEquipe(equipe);
        triagem.setColeta(coleta);
        triagem.setMaterial(material);
        triagem.setStatus(status);
        triagem.setQuantidadeKg(request.quantidadeKg());
        triagem.setQuantidadeRejeitoKg(quantidadeRejeitoKg);
        triagem.setImagemUrl(request.imagemUrl());

        Triagem saved = repository.saveAndFlush(triagem);
        estoque.sincronizar(saved);

        return toResponse(saved);
    }

    public TriagemResponse atualizar(UUID id, TriagemRequest request) {
        Triagem triagem = bloquear(id);
        if (!triagem.getEquipe().getEquipeId().equals(request.equipeId())
                || !triagem.getColeta().getEventoId().equals(request.coletaId())
                || !triagem.getMaterial().getMaterialId().equals(request.materialId()))
            throw new RegraDeNegocioException("Não é permitido alterar a equipe, coleta ou material da triagem.");
        validarQuantidades(request.quantidadeKg(), request.quantidadeRejeitoKg());
        triagem.setQuantidadeKg(request.quantidadeKg());
        triagem.setQuantidadeRejeitoKg(
                request.quantidadeRejeitoKg() != null ? request.quantidadeRejeitoKg() : BigDecimal.ZERO);
        triagem.setImagemUrl(request.imagemUrl());
        if (request.statusId() != null) {
            Status status = statusRepository.findById(request.statusId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Status", request.statusId()));
            validarStatus(status);
            triagem.setStatus(status);
        }
        return salvar(triagem);
    }

    public TriagemResponse atualizarStatus(UUID id, AtualizarStatusTriagemRequest request) {
        Triagem triagem = bloquear(id);
        Status status = statusRepository.findById(request.statusId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Status", request.statusId()));
        validarStatus(status);
        triagem.setStatus(status);
        return salvar(triagem);
    }

    public TriagemResponse concluir(UUID id, ConcluirTriagemRequest request) {
        Triagem triagem = bloquear(id);
        validarQuantidades(request.quantidadeFinalKg(), triagem.getQuantidadeRejeitoKg());
        Status statusConcluida = statusRepository
                .findByReferenciaAndStatusAtual("TRIAGEM", "Concluído")
                .orElseThrow(() -> new RegraDeNegocioException("Status CONCLUIDA não encontrado para TRIAGEM."));
        triagem.setStatus(statusConcluida);
        if (request.quantidadeFinalKg() != null) {
            triagem.setQuantidadeKg(request.quantidadeFinalKg());
        }
        return salvar(triagem);
    }

    public void deletar(UUID id) {
        bloquear(id);
        if (estoque.temMovimentacoes(id))
            throw new RegraDeNegocioException("Triagem com movimentações de estoque não pode ser excluída.");
        repository.deleteById(id);
    }

    private Triagem bloquear(UUID id) {
        estoque.bloquearColetaDaTriagem(id);
        return repository.buscarComBloqueio(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Triagem", id));
    }

    private TriagemResponse salvar(Triagem triagem) {
        validarQuantidades(triagem.getQuantidadeKg(), triagem.getQuantidadeRejeitoKg());
        Triagem saved = repository.saveAndFlush(triagem);
        estoque.sincronizar(saved);
        return toResponse(saved);
    }

    private void validarStatus(Status status) {
        if (status != null && !"TRIAGEM".equals(status.getReferencia()))
            throw new RegraDeNegocioException("Informe um status de triagem.");
    }

    private void validarQuantidades(BigDecimal quantidade, BigDecimal rejeito) {
        if (quantidade == null || quantidade.signum() <= 0 || (rejeito != null && rejeito.signum() < 0))
            throw new RegraDeNegocioException("Peso aproveitável e rejeito inválidos.");
        for (BigDecimal peso : java.util.List.of(quantidade, rejeito == null ? BigDecimal.ZERO : rejeito)) {
            BigDecimal normalizado = peso.stripTrailingZeros();
            if (normalizado.scale() > 3 || normalizado.precision() - normalizado.scale() > 7)
                throw new RegraDeNegocioException("Peso fora dos limites do banco: até 7 dígitos inteiros e 3 decimais.");
        }
    }

    private Triagem findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Triagem", id));
    }

    private TriagemResponse toResponse(Triagem t) {
        List<String> cooperadosNomes = equipeCooperadoRepository
                .findByEquipe_EquipeId(t.getEquipe().getEquipeId())
                .stream()
                .map(ec -> ec.getCooperado().getUsuario().getNome())
                .toList();

        return new TriagemResponse(
                t.getEventoId(),
                t.getEquipe().getEquipeId(),
                t.getEquipe().getNome(),
                t.getColeta().getEventoId(),
                t.getMaterial().getMaterialId(),
                t.getMaterial().getCategoria() != null
                        ? t.getMaterial().getCategoria().getNomeCategoria() : null,
                t.getStatus() != null ? t.getStatus().getStatusAtual() : null,
                t.getQuantidadeKg(),
                t.getQuantidadeRejeitoKg(),
                t.getDataEvento(),
                t.getImagemUrl(),
                cooperadosNomes
        );
    }
}
