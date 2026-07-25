package com.renovai.api.repository;

import com.renovai.api.model.RateioFuncionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RateioFuncionarioRepository extends JpaRepository<RateioFuncionario, Integer> {

    @Query("SELECT rf FROM RateioFuncionario rf " +
           "WHERE rf.cooperado.funcionarioId = :cooperadoId " +
           "ORDER BY rf.rateio.dataRateio DESC")
    List<RateioFuncionario> findByCooperado(@Param("cooperadoId") Integer cooperadoId);

    @Query("SELECT rf FROM RateioFuncionario rf " +
           "WHERE rf.rateio.rateioId = :rateioId " +
           "ORDER BY rf.valorRateio DESC")
    List<RateioFuncionario> findByRateio(@Param("rateioId") Integer rateioId);

    Optional<RateioFuncionario> findByRateio_RateioIdAndCooperado_FuncionarioId(
            Integer rateioId, Integer cooperadoId);

    @Query("SELECT rf FROM RateioFuncionario rf " +
           "WHERE rf.cooperado.funcionarioId = :cooperadoId " +
           "AND rf.rateio.gestor.cooperativa.cooperativaId = :cooperativaId " +
           "ORDER BY rf.rateio.dataRateio DESC")
    List<RateioFuncionario> findByCooperadoECooperativa(
            @Param("cooperadoId") Integer cooperadoId,
            @Param("cooperativaId") Integer cooperativaId);

    long countByRateio_RateioId(Integer rateioId);

    @Query("SELECT COALESCE(SUM(rf.valorRateio), 0) FROM RateioFuncionario rf " +
           "WHERE rf.rateio.rateioId = :rateioId")
    java.math.BigDecimal sumValoresByRateio(@Param("rateioId") Integer rateioId);

    void deleteByCooperado_FuncionarioId(Integer cooperadoId);
}