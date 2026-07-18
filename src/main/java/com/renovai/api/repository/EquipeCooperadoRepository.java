package com.renovai.api.repository;

import com.renovai.api.model.EquipeCooperado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipeCooperadoRepository extends JpaRepository<EquipeCooperado, Integer> {
    List<EquipeCooperado> findByEquipe_EquipeId(Integer equipeId);
    List<EquipeCooperado> findByCooperado_FuncionarioId(Integer funcionarioId);
    boolean existsByEquipe_EquipeIdAndCooperado_FuncionarioId(Integer equipeId, Integer funcionarioId);
    void deleteByEquipe_EquipeIdAndCooperado_FuncionarioId(Integer equipeId, Integer funcionarioId);
}