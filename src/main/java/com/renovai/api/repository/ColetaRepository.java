package com.renovai.api.repository;

import com.renovai.api.model.Coleta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ColetaRepository extends JpaRepository<Coleta, UUID> {
        List<Coleta> findByCooperado_FuncionarioId(UUID cooperadoId);

        @Query("SELECT c FROM Coleta c WHERE c.cooperado.funcionarioId = :cooperadoId AND c.dataEvento BETWEEN :dataInicio AND :dataFim")
        List<Coleta> findColetasPorPeriodo(
                        @Param("cooperadoId") UUID cooperadoId,
                        @Param("dataInicio") LocalDateTime dataInicio,
                        @Param("dataFim") LocalDateTime dataFim);

        @Query("SELECT COUNT(c) FROM Coleta c WHERE c.cooperado.funcionarioId = :cooperadoId AND c.dataEvento BETWEEN :dataInicio AND :dataFim")
        long countColetasPorPeriodo(
                        @Param("cooperadoId") UUID cooperadoId,
                        @Param("dataInicio") LocalDateTime dataInicio,
                        @Param("dataFim") LocalDateTime dataFim);

        @Query("SELECT c FROM Coleta c WHERE c.cooperado.cooperativa.cooperativaId = :cooperativaId ORDER BY c.dataEvento DESC")
        List<Coleta> findByCooperativa(@Param("cooperativaId") UUID cooperativaId);

        @Query("SELECT c FROM Coleta c WHERE c.cooperado.cooperativa.cooperativaId = :cooperativaId AND c.tipoColeta = :tipoColeta")
        List<Coleta> findByCooperativaAndTipo(
                        @Param("cooperativaId") UUID cooperativaId,
                        @Param("tipoColeta") String tipoColeta);

        @Query("SELECT c FROM Coleta c WHERE c.cooperado.cooperativa.cooperativaId = :cooperativaId AND c.status.statusAtual = :status")
        List<Coleta> findByCooperativaAndStatus(
                        @Param("cooperativaId") UUID cooperativaId,
                        @Param("status") String status);

        List<Coleta> findByRota_RotaId(UUID rotaId);

}
