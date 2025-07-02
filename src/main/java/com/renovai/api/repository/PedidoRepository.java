package com.renovai.api.repository;

import com.renovai.api.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Integer> {
    List<Pedido> findByEmpresa_EmpresaId(Integer empresaId);
}
