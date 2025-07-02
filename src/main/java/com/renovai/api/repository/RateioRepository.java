package com.renovai.api.repository;

import com.renovai.api.model.Rateio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RateioRepository extends JpaRepository<Rateio, Integer> {
    List<Rateio> findByGestor_FuncionarioId(Integer gestorId);
}
