package com.renovai.api.repository;

import com.renovai.api.model.Triagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface TriagemRepository extends JpaRepository<Triagem, UUID> {
        List<Triagem> findByColeta_EventoId(UUID coletaId);
        List<Triagem> findByEquipe_EquipeId(UUID equipeId);
    
        @Query("SELECT t FROM Triagem t WHERE t.equipe.equipeId = :equipeId AND t.dataEvento BETWEEN :dataInicio AND :dataFim")
        List<Triagem> findTriagensPorPeriodo(
                @Param("equipeId") UUID equipeId,
                @Param("dataInicio") LocalDateTime dataInicio,
                @Param("dataFim") LocalDateTime dataFim);
    
        @Query("SELECT COUNT(t) FROM Triagem t WHERE t.equipe.equipeId = :equipeId AND t.dataEvento BETWEEN :dataInicio AND :dataFim")
        long countTriagensPorPeriodo(
                @Param("equipeId") UUID equipeId,
                @Param("dataInicio") LocalDateTime dataInicio,
                @Param("dataFim") LocalDateTime dataFim);
}
