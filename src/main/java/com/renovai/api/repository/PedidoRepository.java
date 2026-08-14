package com.renovai.api.repository;

import com.renovai.api.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, UUID> {
    List<Pedido> findByEmpresa_EmpresaId(UUID empresaId);

    @Query("""
            SELECT p FROM Pedido p
            JOIN PedidoCooperativa pc ON pc.pedido.pedidoId = p.pedidoId
            WHERE pc.cooperativa.cooperativaId = :cooperativaId
            ORDER BY p.dataPedido DESC
            """)
    List<Pedido> findByCooperativa(@Param("cooperativaId") UUID cooperativaId);

    @Query("""
            SELECT p FROM Pedido p
            JOIN PedidoCooperativa pc ON pc.pedido.pedidoId = p.pedidoId
            WHERE pc.cooperativa.cooperativaId = :cooperativaId
              AND pc.status.statusAtual = :status
            """)
    List<Pedido> findByCooperativaAndStatus(
            @Param("cooperativaId") UUID cooperativaId,
            @Param("status") String status);

    @Query("""
            SELECT p FROM Pedido p
            JOIN PedidoCooperativa pc ON pc.pedido.pedidoId = p.pedidoId
            WHERE p.empresa.empresaId = :empresaId
              AND pc.status.statusAtual = :status
            """)
    List<Pedido> findByEmpresaAndStatus(
            @Param("empresaId") UUID empresaId,
            @Param("status") String status);

    @Query("""
            SELECT COALESCE(SUM(i.quantidadeKg * i.precoUnitario), 0)
            FROM Pedido p
            JOIN Item i ON i.pedido.pedidoId = p.pedidoId
            JOIN PedidoCooperativa pc ON pc.pedido.pedidoId = p.pedidoId
            WHERE p.empresa.empresaId = :empresaId
              AND pc.status.statusAtual = 'CONCLUIDO'
            """)
    java.math.BigDecimal sumValorAprovadoByEmpresa(@Param("empresaId") UUID empresaId);

    @Query("""
            SELECT COALESCE(SUM(i.quantidadeKg * i.precoUnitario), 0)
            FROM Item i
            JOIN PedidoCooperativa pc ON pc.pedido.pedidoId = i.pedido.pedidoId
            WHERE pc.cooperativa.cooperativaId = :cooperativaId
              AND pc.status.statusAtual = 'CONCLUIDO'
            """)
    java.math.BigDecimal sumValorArrecadadoByCooperativa(@Param("cooperativaId") UUID cooperativaId);
}