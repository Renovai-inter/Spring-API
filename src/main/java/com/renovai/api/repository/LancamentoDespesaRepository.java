package com.renovai.api.repository;
 
import com.renovai.api.model.LancamentoDespesa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
 
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
 
@Repository
public interface LancamentoDespesaRepository extends JpaRepository<LancamentoDespesa, UUID> {
 
    List<LancamentoDespesa> findByDespesa_DespesaId(UUID despesaId);
 
    @Query("""
            SELECT l FROM LancamentoDespesa l
            WHERE l.despesa.cooperativa.cooperativaId = :cooperativaId
              AND l.mesReferencia = :mesReferencia
            ORDER BY l.dataLancamento DESC
            """)
    List<LancamentoDespesa> findByCooperativaAndMes(
            @Param("cooperativaId") UUID cooperativaId,
            @Param("mesReferencia") LocalDate mesReferencia);
 
    @Query("""
            SELECT COALESCE(SUM(l.valor), 0) FROM LancamentoDespesa l
            WHERE l.despesa.cooperativa.cooperativaId = :cooperativaId
              AND l.mesReferencia = :mesReferencia
              AND l.despesa.tipoDespesa = :tipo
            """)
    BigDecimal sumByCooperativaAndMesAndTipo(
            @Param("cooperativaId") UUID cooperativaId,
            @Param("mesReferencia") LocalDate mesReferencia,
            @Param("tipo") String tipo);
}