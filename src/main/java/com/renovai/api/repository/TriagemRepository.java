package com.renovai.api.repository;

import com.renovai.api.model.Triagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TriagemRepository extends JpaRepository<Triagem, Integer> {
    List<Triagem> findByColeta_ColetaId(Integer coletaId);
    List<Triagem> findByEquipe_EquipeId(Integer equipeId);
}
