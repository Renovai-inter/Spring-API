package com.renovai.api.repository;

import com.renovai.api.model.Cooperativa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface CooperativaRepository extends JpaRepository<Cooperativa, UUID> {
    List<Cooperativa> findByNomeContainingIgnoreCase(String nome);

    @Query("SELECT c FROM Cooperativa c WHERE c.numeroCooperados >= :minimo")
    List<Cooperativa> findByNumeroCooperadosMinimo(@Param("minimo") Integer minimo);

    @Query("""
    SELECT DISTINCT c FROM Cooperativa c
    JOIN Perfil p ON p.cooperativa.cooperativaId = c.cooperativaId
    JOIN p.endereco e
    WHERE LOWER(e.cidade) LIKE LOWER(CONCAT('%', :cidade, '%'))
    """)
    List<Cooperativa> findByCidade(@Param("cidade") String cidade);

    @Query("""
        SELECT DISTINCT c FROM Cooperativa c
        JOIN Estoque es ON es.cooperativa.cooperativaId = c.cooperativaId
        JOIN es.material m
        WHERE m.categoria.categoriaId = :categoriaId
        AND es.quantidadeKg > 0
        """)
    List<Cooperativa> findByMaterialDisponivel(@Param("categoriaId") UUID categoriaId);

    @Query("""
        SELECT DISTINCT c FROM Cooperativa c
        JOIN Estoque es ON es.cooperativa.cooperativaId = c.cooperativaId
        JOIN es.material m
        JOIN Perfil p ON p.cooperativa.cooperativaId = c.cooperativaId
        JOIN p.endereco e
        WHERE (:categoriaId IS NULL OR m.categoria.categoriaId = :categoriaId)
        AND (:cidade IS NULL OR LOWER(e.cidade) LIKE LOWER(CONCAT('%', :cidade, '%')))
        AND es.quantidadeKg >= :quantidadeMin
        """)
    List<Cooperativa> buscarComFiltros(
            @Param("categoriaId") UUID categoriaId,
            @Param("cidade") String cidade,
            @Param("quantidadeMin") java.math.BigDecimal quantidadeMin);
}