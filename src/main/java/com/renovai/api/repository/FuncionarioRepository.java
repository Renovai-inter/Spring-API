package com.renovai.api.repository;

import com.renovai.api.model.Funcionario;
import com.renovai.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, UUID> {
    List<Funcionario> findByEstaAtivoTrue();
    Optional<Funcionario> findByUsuario(Usuario usuario);
    List<Funcionario> findByCooperativa_CooperativaId(UUID cooperativaId);
 
    @Query("SELECT f FROM Funcionario f JOIN f.cargo c WHERE c.cargo = :cargo AND f.estaAtivo = true")
    List<Funcionario> findAtivosByCargo(@Param("cargo") String cargo);
 
    @Query("SELECT f FROM Funcionario f WHERE f.cooperativa.cooperativaId = :cooperativaId AND f.estaAtivo = true")
    List<Funcionario> findAtivosByCooperativa(@Param("cooperativaId") UUID cooperativaId);
 
    @Query("SELECT f FROM Funcionario f " +
           "JOIN f.usuario u " +
           "WHERE u.email IS NULL OR u.email = '' " +
           "AND f.estaAtivo = true " +
           "ORDER BY f.dataAdmissao DESC")
    List<Funcionario> findComPreCadastroIncompleto();

    @Query("SELECT f FROM Funcionario f " +
           "JOIN f.usuario u " +
           "WHERE f.cooperativa.cooperativaId = :cooperativaId " +
           "AND (u.email IS NULL OR u.email = '') " +
           "AND f.estaAtivo = true " +
           "ORDER BY f.dataAdmissao DESC")
    List<Funcionario> findComPreCadastroIncompletoByCooperativa(@Param("cooperativaId") UUID cooperativaId);
}