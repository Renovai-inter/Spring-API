package com.renovai.api.repository;
 
import com.renovai.api.model.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
 
import java.util.List;
import java.util.Optional;
import java.util.UUID;
 
@Repository
public interface PerfilRepository extends JpaRepository<Perfil, UUID> {
 
    Optional<Perfil> findByEmail(String email);
 
    boolean existsByEmail(String email);
 
    List<Perfil> findByEstaAtivoTrue();
 
    List<Perfil> findByCooperativa_CooperativaId(UUID cooperativaId);
    @Query("""
            SELECT p FROM Perfil p
            WHERE p.cooperativa.cooperativaId = :cooperativaId
              AND p.estaAtivo = true
            """)
    Optional<Perfil> findByCooperativa_CooperativaIdAtivo(
            @Param("cooperativaId") UUID cooperativaId);
}