package com.renovai.api.repository;
 
import com.renovai.api.model.PedidoCooperativa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;
 
@Repository
public interface PedidoCooperativaRepository extends JpaRepository<PedidoCooperativa, UUID> {
    List<PedidoCooperativa> findByCooperativa_CooperativaId(UUID cooperativaId);
    List<PedidoCooperativa> findByPedido_PedidoId(UUID pedidoId);
}