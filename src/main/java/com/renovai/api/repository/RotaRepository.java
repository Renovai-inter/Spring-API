package com.renovai.api.repository;

import com.renovai.api.model.Rota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface RotaRepository extends JpaRepository<Rota, UUID> {

    List<Rota> findByCooperativa_CooperativaId(UUID cooperativaId);

    List<Rota> findByCooperativa_CooperativaIdAndEstaAtivaTrue(UUID cooperativaId);

    boolean existsByNomeAndCooperativa_CooperativaId(String nome, UUID cooperativaId);
}