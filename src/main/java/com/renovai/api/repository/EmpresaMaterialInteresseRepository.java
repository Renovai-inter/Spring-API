package com.renovai.api.repository;

import com.renovai.api.model.EmpresaMaterialInteresse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmpresaMaterialInteresseRepository extends JpaRepository<EmpresaMaterialInteresse, UUID> {

    List<EmpresaMaterialInteresse> findByEmpresa_EmpresaId(UUID empresaId);

    boolean existsByEmpresa_EmpresaIdAndCategoria_CategoriaId(UUID empresaId, UUID categoriaId);

    Optional<EmpresaMaterialInteresse> findByEmpresa_EmpresaIdAndCategoria_CategoriaId(UUID empresaId, UUID categoriaId);

    void deleteByEmpresa_EmpresaId(UUID empresaId);
}