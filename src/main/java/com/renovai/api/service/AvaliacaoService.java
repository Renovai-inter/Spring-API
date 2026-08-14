package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.AvaliacaoRequest;
import com.renovai.api.dto.response.Responses;
import com.renovai.api.dto.response.Responses.AvaliacaoResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.*;
import com.renovai.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AvaliacaoService {

    private final AvaliacaoRepository repository;
    private final PerfilRepository perfilRepository;
    private final PedidoRepository pedidoRepository;

    public AvaliacaoService(AvaliacaoRepository repository,
                            PerfilRepository perfilRepository,
                            PedidoRepository pedidoRepository) {
        this.repository = repository;
        this.perfilRepository = perfilRepository;
        this.pedidoRepository = pedidoRepository;
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoResponse> listarTodas() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AvaliacaoResponse buscarPorId(UUID id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoResponse> listarPorAvaliado(UUID avaliadoId) {
        return repository.findByAvaliado_PerfilId(avaliadoId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Double mediaNotasPorPerfil(UUID perfilId) {
        return repository.calcularMediaNotasByPerfil(perfilId);
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoResponse> listarPorPedido(UUID pedidoId) {
        return repository.findByPedido_PedidoId(pedidoId).stream().map(this::toResponse).toList();
    }

    public AvaliacaoResponse criar(AvaliacaoRequest request) {
        if (request.avaliadorId().equals(request.avaliadoId())) {
            throw new RegraDeNegocioException("Avaliador e avaliado não podem ser o mesmo perfil.");
        }

        Perfil avaliador = perfilRepository.findById(request.avaliadorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil (avaliador)", request.avaliadorId()));
        Perfil avaliado = perfilRepository.findById(request.avaliadoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil (avaliado)", request.avaliadoId()));

        Pedido pedido = pedidoRepository.findById(request.pedidoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", request.pedidoId()));

        Avaliacao avaliacao = Avaliacao.builder()
                .avaliador(avaliador)
                .avaliado(avaliado)
                .pedido(pedido)
                .nota(request.nota())
                .comentario(request.comentario())
                .build();

        return toResponse(repository.save(avaliacao));
    }

    public void deletar(UUID id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    private Avaliacao findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Avaliação", id));
    }

    private AvaliacaoResponse toResponse(Avaliacao a) {
        return new AvaliacaoResponse(
                a.getAvaliacaoId(),
                a.getAvaliador() != null ? a.getAvaliador().getPerfilId() : null,
                a.getAvaliado() != null ? a.getAvaliado().getPerfilId() : null,
                a.getPedido() != null ? a.getPedido().getPedidoId() : null,
                a.getNota(),
                a.getComentario(),
                a.getDataAvaliacao()
        );
    }

    @Transactional(readOnly = true)
    public Responses.DistribuicaoEstrelas distribuicaoEstrelasPorCooperativa(UUID cooperativaId) {
        long[] contagem = new long[6];
        contagem[0] = repository.contarAvaliacoesSemNota(cooperativaId);
        List<Object[]> rows = repository.contarAvaliacoesPorNotaECooperativa(cooperativaId);
        for (Object[] row : rows) {
            int nota = ((Number) row[0]).intValue();
            long total = ((Number) row[1]).longValue();
            if (nota >= 0 && nota <= 5) {
                contagem[nota] += total;
            }
        }
        return new Responses.DistribuicaoEstrelas(
            contagem[0],
            contagem[1],
            contagem[2],
            contagem[3],
            contagem[4],
            contagem[5]
        );
    }
}