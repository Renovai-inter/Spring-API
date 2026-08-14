package com.renovai.api.repository;

import com.renovai.api.model.NegociacaoMensagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface NegociacaoMensagemRepository extends JpaRepository<NegociacaoMensagem, UUID> {

    List<NegociacaoMensagem> findByNegociacao_NegociacaoIdOrderByDataEnvioAsc(UUID negociacaoId);

    List<NegociacaoMensagem> findByNegociacao_NegociacaoIdAndTipoMensagem(UUID negociacaoId, String tipoMensagem);

    long countByNegociacao_NegociacaoId(UUID negociacaoId);
}