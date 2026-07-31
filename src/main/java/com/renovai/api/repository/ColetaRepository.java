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
   
       // dataColeta não existe — o campo herdado é dataEvento
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

        
}
