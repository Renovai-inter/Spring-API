package com.renovai.api.repository;

import com.renovai.api.model.EmpresaCooperativaFavorita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmpresaCooperativaFavoritaRepository extends JpaRepository<EmpresaCooperativaFavorita, UUID> {

    List<EmpresaCooperativaFavorita> findByEmpresa_EmpresaId(UUID empresaId);

    boolean existsByEmpresa_EmpresaIdAndCooperativa_CooperativaId(UUID empresaId, UUID cooperativaId);

    Optional<EmpresaCooperativaFavorita> findByEmpresa_EmpresaIdAndCooperativa_CooperativaId(UUID empresaId, UUID cooperativaId);

    long countByEmpresa_EmpresaId(UUID empresaId);
}