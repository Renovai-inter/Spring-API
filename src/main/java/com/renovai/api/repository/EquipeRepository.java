package com.renovai.api.repository;

import com.renovai.api.model.Equipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EquipeRepository extends JpaRepository<Equipe, UUID> {
    List<Equipe> findByEstaAtivaTrue();
    List<Equipe> findByGestor_FuncionarioId(UUID gestorId);

    @Query("SELECT e FROM Equipe e WHERE e.gestor.cooperativa.cooperativaId = :cooperativaId")
    List<Equipe> findByCooperativa(@Param("cooperativaId") UUID cooperativaId);

    @Query("SELECT e FROM Equipe e WHERE e.gestor.cooperativa.cooperativaId = :cooperativaId AND e.estaAtiva = true")
    List<Equipe> findByCooperativaAndEstaAtivaTrue(@Param("cooperativaId") UUID cooperativaId);

    @Query("SELECT e FROM Equipe e WHERE e.nome = :nome AND e.gestor.cooperativa.cooperativaId = :cooperativaId")
    Optional<Equipe> findByNomeAndCooperativa(@Param("nome") String nome, @Param("cooperativaId") UUID cooperativaId);

    @Query("SELECT DISTINCT e FROM Equipe e JOIN EquipeCooperado ec ON ec.equipe.equipeId = e.equipeId WHERE ec.cooperado.funcionarioId = :cooperadoId AND e.estaAtiva = true")
    List<Equipe> findEquipesByCooperado(@Param("cooperadoId") UUID cooperadoId);
}