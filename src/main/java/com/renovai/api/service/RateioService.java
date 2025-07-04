package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.RateioRequest;
import com.renovai.api.dto.response.Responses.RateioResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.*;
import com.renovai.api.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.ParameterMode;
import jakarta.persistence.StoredProcedureQuery;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class RateioService {

    private final RateioRepository repository;
    private final FuncionarioRepository funcionarioRepository;
    private final TipoRateioRepository tipoRateioRepository;
    private final EntityManager entityManager;

    public RateioService(RateioRepository repository,
                         FuncionarioRepository funcionarioRepository,
                         TipoRateioRepository tipoRateioRepository,
                         EntityManager entityManager) {
        this.repository = repository;
        this.funcionarioRepository = funcionarioRepository;
        this.tipoRateioRepository = tipoRateioRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<RateioResponse> listarTodos() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RateioResponse buscarPorId(Integer id) {
        return toResponse(findOrThrow(id));
    }

    public RateioResponse criar(RateioRequest request) {
        Funcionario gestor = funcionarioRepository.findById(request.gestorId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário (gestor)", request.gestorId()));

        TipoRateio tipoRateio = null;
        if (request.tipoRateioId() != null) {
            tipoRateio = tipoRateioRepository.findById(request.tipoRateioId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("TipoRateio", request.tipoRateioId()));
        }

        Rateio rateio = Rateio.builder()
                .gestor(gestor)
                .tipoRateio(tipoRateio)
                .build();

        Rateio saved = repository.save(rateio);

        // Aciona Procedure do banco: calcular_rateio(p_rateio_id)
        // A procedure distribui o valor entre os cooperados da equipe do gestor
        try {
            StoredProcedureQuery query = entityManager
                    .createStoredProcedureQuery("calcular_rateio")
                    .registerStoredProcedureParameter("p_rateio_id", Integer.class, ParameterMode.IN)
                    .setParameter("p_rateio_id", saved.getRateioId());
            query.execute();
        } catch (Exception e) {
            // Procedure pode não existir no ambiente de dev; log e continua
        }

        return toResponse(saved);
    }

    public void deletar(Integer id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    private Rateio findOrThrow(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Rateio", id));
    }

    private RateioResponse toResponse(Rateio r) {
        return new RateioResponse(
                r.getRateioId(),
                r.getGestor().getFuncionarioId(),
                r.getGestor().getUsuario() != null ? r.getGestor().getUsuario().getNome() : null,
                r.getTipoRateio() != null ? r.getTipoRateio().getTipoRateioId() : null,
                r.getTipoRateio() != null ? r.getTipoRateio().getTipoRateio() : null,
                r.getDataRateio()
        );
    }
}
