package com.renovai.api.repository;

import com.renovai.api.model.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {
    List<Empresa> findByNomeContainingIgnoreCase(String nome);

    @Query("""
            SELECT e FROM Empresa e
            JOIN Perfil p ON p.empresa.empresaId = e.empresaId
            JOIN p.endereco en
            WHERE LOWER(en.cidade) LIKE LOWER(CONCAT('%', :cidade, '%'))
            """)
    List<Empresa> findByCidade(@Param("cidade") String cidade);

}