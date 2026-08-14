package com.renovai.api.repository;

import com.renovai.api.model.RotaEndereco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface RotaEnderecoRepository extends JpaRepository<RotaEndereco, UUID> {

    List<RotaEndereco> findByRota_RotaIdOrderByOrdemAsc(UUID rotaId);

    boolean existsByRota_RotaIdAndEndereco_EnderecoId(UUID rotaId, UUID enderecoId);

    void deleteByRota_RotaId(UUID rotaId);

    @Query("SELECT COALESCE(MAX(re.ordem), 0) FROM RotaEndereco re WHERE re.rota.rotaId = :rotaId")
    Integer findMaxOrdemByRota(@Param("rotaId") UUID rotaId);
}