package com.renovai.api.repository;

import com.renovai.api.model.NegociacaoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface NegociacaoItemRepository extends JpaRepository<NegociacaoItem, UUID> {

    List<NegociacaoItem> findByNegociacao_NegociacaoId(UUID negociacaoId);

    void deleteByNegociacao_NegociacaoId(UUID negociacaoId);

    @Query("SELECT COALESCE(SUM(ni.quantidadeKg * ni.precoUnitario), 0) FROM NegociacaoItem ni WHERE ni.negociacao.negociacaoId = :negociacaoId")
    BigDecimal sumValorByNegociacao(@Param("negociacaoId") UUID negociacaoId);
}