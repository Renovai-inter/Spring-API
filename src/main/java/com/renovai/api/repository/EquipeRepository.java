package com.renovai.api.repository;

import com.renovai.api.model.Equipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EquipeRepository extends JpaRepository<Equipe, Integer> {
    
    List<Equipe> findByCooperativa_CooperativaId(Integer cooperativaId);
    
    List<Equipe> findByEstaAtivaTrue();
    
    List<Equipe> findByCooperativa_CooperativaIdAndEstaAtivaTrue(Integer cooperativaId);
    
    List<Equipe> findByGestor_FuncionarioId(Integer gestorId);
    
    Optional<Equipe> findByNomeAndCooperativa_CooperativaId(String nome, Integer cooperativaId);
    
    @Query("SELECT DISTINCT e FROM Equipe e " +
           "JOIN EquipeCooperado ec ON e.equipeId = ec.equipe.equipeId " +
           "WHERE ec.cooperado.funcionarioId = :cooperadoId AND e.estaAtiva = true")
    List<Equipe> findEquipesByCooperado(@Param("cooperadoId") Integer cooperadoId);
}