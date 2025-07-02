package com.renovai.api.repository;

import com.renovai.api.model.Equipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EquipeRepository extends JpaRepository<Equipe, Integer> {
    List<Equipe> findByCooperativa_CooperativaId(Integer cooperativaId);
    List<Equipe> findByEstaAtivaTrue();
}
