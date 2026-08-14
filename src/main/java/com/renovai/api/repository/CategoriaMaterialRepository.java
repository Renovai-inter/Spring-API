package com.renovai.api.repository;

import com.renovai.api.model.CategoriaMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CategoriaMaterialRepository extends JpaRepository<CategoriaMaterial, UUID> {
    java.util.List<CategoriaMaterial> findByNomeCategoriaContainingIgnoreCase(String nome);

    @Query("SELECT fn_total_kg_por_categoria(:categoriaId, :dataInicio, :dataFim)")
    long fn_total_kg_por_categoria(
            @Param("categoriaId") UUID categoriaId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

    List<CategoriaMaterial> findByCategoriaPaiIsNull();

    List<CategoriaMaterial> findByCategoriaPai_CategoriaId(UUID categoriaPaiId);

    boolean existsByNomeCategoriaAndCategoriaPai_CategoriaId(String nomeCategoria, UUID categoriaPaiId);
}