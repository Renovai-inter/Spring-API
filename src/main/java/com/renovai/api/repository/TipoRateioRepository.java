package com.renovai.api.repository;

import com.renovai.api.model.TipoRateio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TipoRateioRepository extends JpaRepository<TipoRateio, UUID> {
    Optional<TipoRateio> findByTipoRateio(String tipoRateio);
    boolean existsByTipoRateio(String tipoRateio);
}