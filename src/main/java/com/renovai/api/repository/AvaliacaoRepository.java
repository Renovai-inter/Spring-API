package com.renovai.api.repository;

import com.renovai.api.model.Avaliacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, UUID> {
    List<Avaliacao> findByAvaliado_PerfilId(UUID avaliadoId);

    @Query("SELECT AVG(a.nota) FROM Avaliacao a WHERE a.avaliado.perfilId = :perfilId AND a.nota IS NOT NULL")
    Double calcularMediaNotasByPerfil(@Param("perfilId") UUID perfilId);

    List<Avaliacao> findByPedido_PedidoId(UUID pedidoId);
}
