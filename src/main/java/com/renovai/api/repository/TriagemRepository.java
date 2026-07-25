package com.renovai.api.repository;

import com.renovai.api.model.Triagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TriagemRepository extends JpaRepository<Triagem, Integer> {
    List<Triagem> findByColeta_ColetaId(Integer coletaId);
    List<Triagem> findByEquipe_EquipeId(Integer equipeId);

        @Query("SELECT t FROM Triagem t " +
           "WHERE t.equipe.equipeId = :equipeId " +
           "AND t.dataTriagem BETWEEN :dataInicio AND :dataFim")
    List<Triagem> findTriagensPorPeriodo(
            @Param("equipeId") Integer equipeId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

    @Query("SELECT COUNT(t) FROM Triagem t " +
           "WHERE t.equipe.equipeId = :equipeId " +
           "AND t.dataTriagem BETWEEN :dataInicio AND :dataFim")
    long countTriagensPorPeriodo(
            @Param("equipeId") Integer equipeId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);
}
