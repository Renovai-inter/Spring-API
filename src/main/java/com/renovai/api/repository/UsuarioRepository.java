package com.renovai.api.repository;

import com.renovai.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByCpf(String cpf);
    boolean existsByCpf(String cpf);
    Optional<Usuario> findByEmail(String email);
}