package com.renovai.api.repository;

import com.renovai.api.model.Estoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EstoqueRepository extends JpaRepository<Estoque, UUID> {
    List<Estoque> findByCooperativa_CooperativaId(UUID cooperativaId);

    Optional<Estoque> findByCooperativa_CooperativaIdAndMaterial_MaterialId(UUID cooperativaId, UUID materialId);

    @Query("SELECT e FROM Estoque e WHERE e.quantidadeKg > 0 AND e.cooperativa.cooperativaId = :cooperativaId")
    List<Estoque> findDisponiveisByCooperativa(@Param("cooperativaId") UUID cooperativaId);

    @Query("""
            SELECT e FROM Estoque e
            WHERE e.material.categoria.categoriaId = :categoriaId
              AND e.quantidadeKg >= :quantidadeMin
              AND e.cooperativa.cooperativaId IS NOT NULL
            ORDER BY e.quantidadeKg DESC
            """)
    List<Estoque> findDisponivelByCategoria(
            @Param("categoriaId") UUID categoriaId,
            @Param("quantidadeMin") java.math.BigDecimal quantidadeMin);

    @Query("""
            SELECT e FROM Estoque e
            JOIN Perfil p ON p.cooperativa.cooperativaId = e.cooperativa.cooperativaId
            JOIN p.endereco en
            WHERE e.material.categoria.categoriaId = :categoriaId
              AND e.quantidadeKg >= :quantidadeMin
              AND (:cidade IS NULL OR LOWER(en.cidade) LIKE LOWER(CONCAT('%', :cidade, '%')))
            ORDER BY e.quantidadeKg DESC
            """)
    List<Estoque> buscarComFiltros(
            @Param("categoriaId") UUID categoriaId,
            @Param("quantidadeMin") java.math.BigDecimal quantidadeMin,
            @Param("cidade") String cidade);
    @Query("SELECT COALESCE(SUM(e.quantidadeKg), 0) FROM Estoque e WHERE e.cooperativa.cooperativaId = :cooperativaId")
    java.math.BigDecimal sumQuantidadeByCooperativa(@Param("cooperativaId") UUID cooperativaId);
}
