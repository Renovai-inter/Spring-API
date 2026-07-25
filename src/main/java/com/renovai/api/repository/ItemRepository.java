package com.renovai.api.repository;

import com.renovai.api.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {
    List<Item> findByPedido_PedidoId(Integer pedidoId);

    @Query("SELECT i FROM Item i " +
           "WHERE i.pedido.dataPedido BETWEEN :dataInicio AND :dataFim")
    List<Item> findItensPorPeriodo(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

    @Query("SELECT i FROM Item i " +
           "JOIN PedidoCooperativa pc ON i.pedido.pedidoId = pc.pedido.pedidoId " +
           "WHERE pc.cooperativa.cooperativaId = :cooperativaId " +
           "AND i.pedido.dataPedido BETWEEN :dataInicio AND :dataFim")
    List<Item> findItensPorCooperativaEPeriodo(
            @Param("cooperativaId") Integer cooperativaId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

    @Query("SELECT COALESCE(SUM(i.quantidadeKg * i.precoUnitario), 0) FROM Item i " +
           "WHERE i.pedido.dataPedido BETWEEN :dataInicio AND :dataFim")
    BigDecimal sumValoresPorPeriodo(
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

    @Query("SELECT COALESCE(SUM(i.quantidadeKg * i.precoUnitario), 0) FROM Item i " +
           "JOIN PedidoCooperativa pc ON i.pedido.pedidoId = pc.pedido.pedidoId " +
           "WHERE pc.cooperativa.cooperativaId = :cooperativaId " +
           "AND i.pedido.dataPedido BETWEEN :dataInicio AND :dataFim")
    BigDecimal sumValoresPorCooperativaEPeriodo(
            @Param("cooperativaId") Integer cooperativaId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);
}
