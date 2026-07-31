package com.renovai.api.repository;

import com.renovai.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {
    Optional<Usuario> findByCpf(String cpf);
    boolean existsByCpf(String cpf);
    boolean existsByEmail(String email); // adicionar — usado no UsuarioService
    Optional<Usuario> findByEmail(String email);
}