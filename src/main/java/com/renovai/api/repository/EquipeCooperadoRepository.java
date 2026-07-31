package com.renovai.api.repository;

import com.renovai.api.model.EquipeCooperado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EquipeCooperadoRepository extends JpaRepository<EquipeCooperado, UUID> {
    List<EquipeCooperado> findByEquipe_EquipeId(UUID equipeId);
    List<EquipeCooperado> findByCooperado_FuncionarioId(UUID funcionarioId);
    boolean existsByEquipe_EquipeIdAndCooperado_FuncionarioId(UUID equipeId, UUID funcionarioId);
    void deleteByEquipe_EquipeIdAndCooperado_FuncionarioId(UUID equipeId, UUID funcionarioId);
}