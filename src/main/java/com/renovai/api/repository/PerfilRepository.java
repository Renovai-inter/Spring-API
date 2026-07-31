package com.renovai.api.repository;

import com.renovai.api.model.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface PerfilRepository extends JpaRepository<Perfil, UUID> {
    Optional<Perfil> findByEmail(String email);
    boolean existsByEmail(String email);
    List<Perfil> findByEstaAtivoTrue();
}