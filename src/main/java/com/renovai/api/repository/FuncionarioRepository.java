package com.renovai.api.repository;

import com.renovai.api.model.Avaliacao;
import com.renovai.api.model.Funcionario;
import com.renovai.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, Integer> {
    List<Funcionario> findByEstaAtivoTrue();
    Optional<Funcionario> findByUsuario(Usuario usuario);

    @Query("SELECT f FROM Funcionario f JOIN f.cargo c WHERE c.cargo = :cargo AND f.estaAtivo = true")
    List<Funcionario> findAtivosByCargo(@Param("cargo") String cargo);

}