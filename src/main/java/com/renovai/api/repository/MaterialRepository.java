package com.renovai.api.repository;

import com.renovai.api.model.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MaterialRepository extends JpaRepository<Material, Integer> {
    List<Material> findByEstaDisponivelTrue();
    List<Material> findByCategoriaContainingIgnoreCase(String categoria);
}
