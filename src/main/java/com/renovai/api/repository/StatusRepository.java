package com.renovai.api.repository;

import com.renovai.api.model.Status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StatusRepository extends JpaRepository<Status, UUID> {
    List<Status> findByReferencia(String referencia);

    Optional<Status> findByReferenciaAndStatusAtual(String referencia, String statusAtual);
}