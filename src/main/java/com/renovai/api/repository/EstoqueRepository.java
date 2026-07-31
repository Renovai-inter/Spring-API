package com.renovai.api.repository;

import com.renovai.api.model.Estoque;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EstoqueRepository extends JpaRepository<Estoque, UUID> {
    List<Estoque> findByCooperativa_CooperativaId(UUID cooperativaId);

    Optional<Estoque> findByCooperativa_CooperativaIdAndMaterial_MaterialId(UUID cooperativaId, UUID materialId);

    @Query("SELECT e FROM Estoque e WHERE e.quantidadeKg > 0 AND e.cooperativa.cooperativaId = :cooperativaId")
    List<Estoque> findDisponiveisByCooperativa(@Param("cooperativaId") UUID cooperativaId);
}
