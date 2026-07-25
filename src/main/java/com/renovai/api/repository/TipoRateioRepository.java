package com.renovai.api.repository;

import com.renovai.api.model.TipoRateio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoRateioRepository extends JpaRepository<TipoRateio, Integer> {

    Optional<TipoRateio> findByTipoRateio(String tipoRateio);

    boolean existsByTipoRateio(String tipoRateio);
}