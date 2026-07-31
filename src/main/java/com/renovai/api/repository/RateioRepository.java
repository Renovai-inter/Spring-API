package com.renovai.api.repository;

import com.renovai.api.model.Rateio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RateioRepository extends JpaRepository<Rateio, UUID> {
       List<Rateio> findByGestor_FuncionarioId(UUID gestorId);
   
       @Query("SELECT r FROM Rateio r WHERE r.gestor.cooperativa.cooperativaId = :cooperativaId ORDER BY r.dataRateio DESC")
       List<Rateio> findByCooperativa(@Param("cooperativaId") UUID cooperativaId);
   
       List<Rateio> findByTipoRateio_TipoRateioId(UUID tipoRateioId);
   
       @Query("SELECT r FROM Rateio r WHERE r.dataRateio BETWEEN :dataInicio AND :dataFim ORDER BY r.dataRateio DESC")
       List<Rateio> findRateiosPorPeriodo(
               @Param("dataInicio") LocalDateTime dataInicio,
               @Param("dataFim") LocalDateTime dataFim);
   
       @Query("SELECT r FROM Rateio r WHERE r.gestor.cooperativa.cooperativaId = :cooperativaId AND r.dataRateio BETWEEN :dataInicio AND :dataFim ORDER BY r.dataRateio DESC")
       List<Rateio> findRateiosPorCooperativaEPeriodo(
               @Param("cooperativaId") UUID cooperativaId,
               @Param("dataInicio") LocalDateTime dataInicio,
               @Param("dataFim") LocalDateTime dataFim);
   
       @Query("SELECT r FROM Rateio r WHERE r.gestor.cooperativa.cooperativaId = :cooperativaId ORDER BY r.dataRateio DESC LIMIT 1")
       Optional<Rateio> findUltimoRateioDaCooperativa(@Param("cooperativaId") UUID cooperativaId);
}
