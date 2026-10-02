package com.renovai.api.repository;
 
import com.renovai.api.model.PedidoCooperativa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
 
@Repository
public interface PedidoCooperativaRepository extends JpaRepository<PedidoCooperativa, UUID> {
    @org.springframework.data.jpa.repository.Query("""
        select (count(n) > 0) from Negociacao n, PedidoCooperativa pc
        where pc.pedidoCooperativaId=:id and n.pedido=pc.pedido and n.cooperativa=pc.cooperativa
        """)
    boolean existeNegociacao(@org.springframework.data.repository.query.Param("id") UUID id);
    List<PedidoCooperativa> findByCooperativa_CooperativaId(UUID cooperativaId);
    List<PedidoCooperativa> findByPedido_PedidoId(UUID pedidoId);
}
