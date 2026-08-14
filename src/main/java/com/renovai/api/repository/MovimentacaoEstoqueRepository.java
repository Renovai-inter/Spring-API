package com.renovai.api.repository;

import com.renovai.api.model.MovimentacaoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, UUID> {

    List<MovimentacaoEstoque> findByEstoque_EstoqueIdOrderByDataMovimentacaoDesc(UUID estoqueId);

    @Query("""
        SELECT m FROM MovimentacaoEstoque m
        WHERE m.estoque.cooperativa.cooperativaId = :cooperativaId
        ORDER BY m.dataMovimentacao DESC
        """)
    List<MovimentacaoEstoque> findByCooperativa(@Param("cooperativaId") UUID cooperativaId);

    @Query("""
        SELECT m FROM MovimentacaoEstoque m
        WHERE m.estoque.cooperativa.cooperativaId = :cooperativaId
          AND m.dataMovimentacao BETWEEN :inicio AND :fim
        ORDER BY m.dataMovimentacao DESC
        """)
    List<MovimentacaoEstoque> findByCooperativaAndPeriodo(
            @Param("cooperativaId") UUID cooperativaId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);

    @Query("""
        SELECT m FROM MovimentacaoEstoque m
        WHERE m.estoque.cooperativa.cooperativaId = :cooperativaId
          AND m.tipoMovimentacao = 'ENTRADA'
        """)
    List<MovimentacaoEstoque> findEntradasByCooperativa(@Param("cooperativaId") UUID cooperativaId);

    @Query("""
        SELECT m FROM MovimentacaoEstoque m
        WHERE m.estoque.cooperativa.cooperativaId = :cooperativaId
          AND m.tipoMovimentacao = 'SAIDA'
        """)
    List<MovimentacaoEstoque> findSaidasByCooperativa(@Param("cooperativaId") UUID cooperativaId);
}