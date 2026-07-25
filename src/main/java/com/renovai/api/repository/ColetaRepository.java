package com.renovai.api.repository;

import com.renovai.api.model.Coleta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ColetaRepository extends JpaRepository<Coleta, Integer> {
    List<Coleta> findByCooperado_FuncionarioId(Integer cooperadoId);

    @Query("SELECT c FROM Coleta c " +
           "WHERE c.cooperado.funcionarioId = :cooperadoId " +
           "AND c.dataColeta BETWEEN :dataInicio AND :dataFim")
    List<Coleta> findColetasPorPeriodo(
            @Param("cooperadoId") Integer cooperadoId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

    @Query("SELECT COUNT(c) FROM Coleta c " +
           "WHERE c.cooperado.funcionarioId = :cooperadoId " +
           "AND c.dataColeta BETWEEN :dataInicio AND :dataFim")
    long countColetasPorPeriodo(
            @Param("cooperadoId") Integer cooperadoId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);
}
