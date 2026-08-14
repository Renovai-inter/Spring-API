package com.renovai.api.service;
 
import com.renovai.api.dto.request.Requests.CooperativaRequest;
import com.renovai.api.dto.response.Responses.CooperativaPerfilPublicoResponse;
import com.renovai.api.dto.response.Responses.CooperativaResponse;
import com.renovai.api.dto.response.Responses.EstoqueResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.Cooperativa;
import com.renovai.api.model.Perfil;
import com.renovai.api.repository.AvaliacaoRepository;
import com.renovai.api.repository.CooperativaRepository;
import com.renovai.api.repository.EstoqueRepository;
import com.renovai.api.repository.PerfilRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
 
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
 
@Service
@Transactional
public class CooperativaService {
 
    private final CooperativaRepository repository;
    private final PerfilRepository perfilRepository;
    private final EstoqueRepository estoqueRepository;
    private final AvaliacaoRepository avaliacaoRepository;
 
    public CooperativaService(CooperativaRepository repository,
                               PerfilRepository perfilRepository,
                               EstoqueRepository estoqueRepository,
                               AvaliacaoRepository avaliacaoRepository) {
        this.repository = repository;
        this.perfilRepository = perfilRepository;
        this.estoqueRepository = estoqueRepository;
        this.avaliacaoRepository = avaliacaoRepository;
    }
 
    @Transactional(readOnly = true)
    public List<CooperativaResponse> listarTodas() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }
 
    @Transactional(readOnly = true)
    public CooperativaResponse buscarPorId(UUID id) {
        return toResponse(findOrThrow(id));
    }
 
    @Transactional(readOnly = true)
    public List<CooperativaResponse> buscarPorNome(String nome) {
        return repository.findByNomeContainingIgnoreCase(nome).stream()
                .map(this::toResponse).toList();
    }
 
    @Transactional(readOnly = true)
    public List<CooperativaResponse> buscarComFiltros(UUID categoriaId, String cidade, BigDecimal quantidadeMin) {
        return repository.buscarComFiltros(
                categoriaId,
                cidade,
                quantidadeMin != null ? quantidadeMin : BigDecimal.ZERO
        ).stream().map(this::toResponse).toList();
    }
 
    @Transactional(readOnly = true)
    public CooperativaPerfilPublicoResponse buscarPerfilPublico(UUID cooperativaId) {
        Cooperativa c = findOrThrow(cooperativaId);
 
        Perfil perfil = perfilRepository.findByCooperativa_CooperativaIdAtivo(cooperativaId)
                .orElse(null);
 
        String cidade = (perfil != null && perfil.getEndereco() != null)
                ? perfil.getEndereco().getCidade() : null;
 
        Double media = perfil != null
                ? avaliacaoRepository.calcularMediaNotasByPerfil(perfil.getPerfilId()) : null;
 
        Long totalAvaliacoes = perfil != null
                ? (long) avaliacaoRepository.findByAvaliado_PerfilId(perfil.getPerfilId()).size()
                : 0L;
 
        List<EstoqueResponse> estoques = estoqueRepository
                .findDisponiveisByCooperativa(cooperativaId)
                .stream()
                .map(e -> new EstoqueResponse(
                        e.getEstoqueId(),
                        e.getCooperativa().getCooperativaId(),
                        e.getCooperativa().getNome(),
                        e.getMaterial().getMaterialId(),
                        e.getMaterial().getCategoria() != null
                                ? e.getMaterial().getCategoria().getNomeCategoria() : null,
                        e.getQuantidadeKg(),
                        e.getDataAtualizacao()
                ))
                .toList();
 
        return new CooperativaPerfilPublicoResponse(
                c.getCooperativaId(),
                c.getNome(),
                c.getDescricao(),
                c.getImagemUrl(),
                c.getContatoPreferencial(),
                c.getHorarioFuncionamento(),
                cidade,
                media,
                totalAvaliacoes,
                estoques
        );
    }
 
    public CooperativaResponse criar(CooperativaRequest request) {
        Cooperativa cooperativa = Cooperativa.builder()
                .nome(request.nome())
                .descricao(request.descricao())
                .numeroCooperados(request.numeroCooperados() != null ? request.numeroCooperados() : 0)
                .horarioFuncionamento(request.horarioFuncionamento())
                .imagemUrl(request.imagemUrl())
                .contatoPreferencial(request.contatoPreferencial())
                .build();
        return toResponse(repository.save(cooperativa));
    }
 
    public CooperativaResponse atualizar(UUID id, CooperativaRequest request) {
        Cooperativa cooperativa = findOrThrow(id);
        cooperativa.setNome(request.nome());
        cooperativa.setDescricao(request.descricao());
        if (request.numeroCooperados() != null) cooperativa.setNumeroCooperados(request.numeroCooperados());
        cooperativa.setHorarioFuncionamento(request.horarioFuncionamento());
        cooperativa.setImagemUrl(request.imagemUrl());
        cooperativa.setContatoPreferencial(request.contatoPreferencial());
        cooperativa.setDataAtualizacao(java.time.LocalDateTime.now());
        return toResponse(repository.save(cooperativa));
    }
 
    public void deletar(UUID id) {
        findOrThrow(id);
        repository.deleteById(id);
    }
 
    private Cooperativa findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", id));
    }
 
    private String resolverCidade(UUID cooperativaId) {
        return perfilRepository.findByCooperativa_CooperativaIdAtivo(cooperativaId)
                .filter(p -> p.getEndereco() != null)
                .map(p -> p.getEndereco().getCidade())
                .orElse(null);
    }
 
    private CooperativaResponse toResponse(Cooperativa c) {
        return new CooperativaResponse(
                c.getCooperativaId(),
                c.getNome(),
                c.getDescricao(),
                c.getNumeroCooperados(),
                c.getHorarioFuncionamento(),
                c.getImagemUrl(),
                c.getContatoPreferencial(),
                resolverCidade(c.getCooperativaId())
        );
    }
}