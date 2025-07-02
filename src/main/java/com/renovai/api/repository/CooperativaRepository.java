package com.renovai.api.repository;

import com.renovai.api.model.Cooperativa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CooperativaRepository extends JpaRepository<Cooperativa, Integer> {
    List<Cooperativa> findByNomeContainingIgnoreCase(String nome);

    @Query("SELECT c FROM Cooperativa c WHERE c.numeroCooperados >= :minimo")
    List<Cooperativa> findByNumeroCooperadosMinimo(@Param("minimo") Integer minimo);
}
