package com.renovai.api.repository;

import com.renovai.api.model.RateioFuncionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RateioFuncionarioRepository extends JpaRepository<RateioFuncionario, UUID> {

       @Query("SELECT rf FROM RateioFuncionario rf WHERE rf.cooperado.funcionarioId = :cooperadoId ORDER BY rf.rateio.dataRateio DESC")
       List<RateioFuncionario> findByCooperado(@Param("cooperadoId") UUID cooperadoId);
   
       @Query("SELECT rf FROM RateioFuncionario rf WHERE rf.rateio.rateioId = :rateioId ORDER BY rf.valorRateio DESC")
       List<RateioFuncionario> findByRateio(@Param("rateioId") UUID rateioId);
   
       Optional<RateioFuncionario> findByRateio_RateioIdAndCooperado_FuncionarioId(UUID rateioId, UUID cooperadoId);
   
       @Query("SELECT rf FROM RateioFuncionario rf WHERE rf.cooperado.funcionarioId = :cooperadoId AND rf.rateio.gestor.cooperativa.cooperativaId = :cooperativaId ORDER BY rf.rateio.dataRateio DESC")
       List<RateioFuncionario> findByCooperadoECooperativa(
               @Param("cooperadoId") UUID cooperadoId,
               @Param("cooperativaId") UUID cooperativaId);
   
       long countByRateio_RateioId(UUID rateioId);
   
       @Query("SELECT COALESCE(SUM(rf.valorRateio), 0) FROM RateioFuncionario rf WHERE rf.rateio.rateioId = :rateioId")
       BigDecimal sumValoresByRateio(@Param("rateioId") UUID rateioId);
   
       void deleteByCooperado_FuncionarioId(UUID cooperadoId);
}