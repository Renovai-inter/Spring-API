package com.renovai.api.repository;

import com.renovai.api.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {
    List<Item> findByPedido_PedidoId(Integer pedidoId);
}
