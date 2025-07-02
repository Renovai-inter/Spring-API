package com.renovai.api.repository;

import com.renovai.api.model.PedidoCooperativa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PedidoCooperativaRepository extends JpaRepository<PedidoCooperativa, Integer> {
    List<PedidoCooperativa> findByCooperativa_CooperativaId(Integer cooperativaId);
    List<PedidoCooperativa> findByPedido_PedidoId(Integer pedidoId);
}
