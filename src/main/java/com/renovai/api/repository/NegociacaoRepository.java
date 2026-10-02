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

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Negociacao n where n.negociacaoId = :id")
    Optional<Negociacao> buscarComBloqueio(@Param("id") UUID id);

    List<Negociacao> findByPedido_PedidoId(UUID pedidoId);

    List<Negociacao> findByCooperativa_CooperativaId(UUID cooperativaId);
    List<Negociacao> findByEmpresa_EmpresaId(UUID empresaId);

    Optional<Negociacao> findByPedido_PedidoIdAndCooperativa_CooperativaId(UUID pedidoId, UUID cooperativaId);

    List<Negociacao> findByCooperativa_CooperativaIdAndStatus_StatusAtual(UUID cooperativaId, String statusAtual);

    List<Negociacao> findByEmpresa_EmpresaIdAndStatus_StatusAtual(UUID empresaId, String statusAtual);

    @Query(value = """
            select count(distinct p.pedido_id) from pedidos p
            join pedidos_cooperativas pc on pc.pedido_id=p.pedido_id
            join status s on s.status_id=pc.status_id where p.empresa_id=:empresaId
            and lower(s.status_atual) in ('aceito','finalizado','concluído','concluido')
            """, nativeQuery = true)
    Long countAceitosByEmpresa(@Param("empresaId") UUID empresaId);

    @Query(value = """
            select coalesce(sum(coalesce(n.valor_total,
            (select sum(i.quantidade_kg*i.preco_unitario) from pedido_itens i where i.pedido_id=p.pedido_id))),0)
            from pedidos p join pedidos_cooperativas pc on pc.pedido_id=p.pedido_id
            join status s on s.status_id=pc.status_id
            left join negociacoes n on n.pedido_id=pc.pedido_id and n.cooperativa_id=pc.cooperativa_id
            where p.empresa_id=:empresaId and lower(s.status_atual) in ('aceito','finalizado','concluído','concluido')
            """, nativeQuery = true)
    java.math.BigDecimal sumValorNegociadoByEmpresa(@Param("empresaId") UUID empresaId);
}
