package com.renovai.api.repository;

import com.renovai.api.model.Negociacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NegociacaoRepository extends JpaRepository<Negociacao, UUID> {

    List<Negociacao> findByPedido_PedidoId(UUID pedidoId);

    List<Negociacao> findByCooperativa_CooperativaId(UUID cooperativaId);
    List<Negociacao> findByEmpresa_EmpresaId(UUID empresaId);

    Optional<Negociacao> findByPedido_PedidoIdAndCooperativa_CooperativaId(UUID pedidoId, UUID cooperativaId);

    List<Negociacao> findByCooperativa_CooperativaIdAndStatus_StatusAtual(UUID cooperativaId, String statusAtual);

    List<Negociacao> findByEmpresa_EmpresaIdAndStatus_StatusAtual(UUID empresaId, String statusAtual);

    @Query("SELECT COUNT(n) FROM Negociacao n WHERE n.empresa.empresaId = :empresaId AND n.status.statusAtual = 'ACEITO'")
    Long countAceitosByEmpresa(@Param("empresaId") UUID empresaId);

    @Query("SELECT COALESCE(SUM(n.valorTotal), 0) FROM Negociacao n WHERE n.empresa.empresaId = :empresaId AND n.status.statusAtual = 'CONCLUIDO'")
    java.math.BigDecimal sumValorNegociadoByEmpresa(@Param("empresaId") UUID empresaId);
}