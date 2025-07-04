package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.AvaliacaoRequest;
import com.renovai.api.dto.response.Responses.AvaliacaoResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.*;
import com.renovai.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public AvaliacaoResponse buscarPorId(Integer id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoResponse> listarPorAvaliado(Integer avaliadoId) {
        return repository.findByAvaliado_PerfilId(avaliadoId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Double mediaNotasPorPerfil(Integer perfilId) {
        return repository.calcularMediaNotasByPerfil(perfilId);
    }

    public AvaliacaoResponse criar(AvaliacaoRequest request) {
        if (request.avaliadorId().equals(request.avaliadoId())) {
            throw new RegraDeNegocioException("Avaliador e avaliado não podem ser o mesmo perfil.");
        }

        Perfil avaliador = perfilRepository.findById(request.avaliadorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil (avaliador)", request.avaliadorId()));
        Perfil avaliado = perfilRepository.findById(request.avaliadoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil (avaliado)", request.avaliadoId()));

        Pedido pedido = null;
        if (request.pedidoId() != null) {
            pedido = pedidoRepository.findById(request.pedidoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", request.pedidoId()));
        }

        Avaliacao avaliacao = Avaliacao.builder()
                .avaliador(avaliador)
                .avaliado(avaliado)
                .pedido(pedido)
                .nota(request.nota())
                .comentario(request.comentario())
                .build();

        return toResponse(repository.save(avaliacao));
    }

    public void deletar(Integer id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    private Avaliacao findOrThrow(Integer id) {
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
}
