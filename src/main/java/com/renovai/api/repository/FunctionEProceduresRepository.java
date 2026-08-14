package com.renovai.api.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface FunctionEProceduresRepository extends JpaRepository<Object, UUID> {
    @Query(value = "SELECT fn_calcular_media_avaliacoes(:perfilId)", nativeQuery = true)
    BigDecimal calcularMediaAvaliacoes(
            @Param("perfilId") UUID perfilId);

    @Query(value = "SELECT fn_calcular_numero_cooperados(:cooperativaId)", nativeQuery = true)
    Integer calcularNumeroCooperados(
            @Param("cooperativaId") UUID cooperativaId);

    @Query(value = "SELECT fn_calcular_quantidade_rejeito_kg(:triagemQuantidadeKg, :coletaId)", nativeQuery = true)
    BigDecimal calcularQuantidadeRejeitoKg(
            @Param("triagemQuantidadeKg") BigDecimal triagemQuantidadeKg,
            @Param("coletaId") UUID coletaId);

    @Query(value = "SELECT fn_calcular_rateio_automatico(:cooperativaId, :mesReferencia)", nativeQuery = true)
    BigDecimal calcularRateioAutomatico(
            @Param("cooperativaId") UUID cooperativaId,
            @Param("mesReferencia") LocalDateTime mesReferencia);

    @Query(value = "SELECT fn_calcular_total_acumulado(:cooperativaId, :dataInicio, :dataFim)", nativeQuery = true)
    BigDecimal calcularTotalAcumulado(
            @Param("cooperativaId") UUID cooperativaId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

    @Query(value = "SELECT fn_calcular_total_liquido(:cooperativaId, :dataInicio, :dataFim)", nativeQuery = true)
    BigDecimal calcularTotalLiquido(
            @Param("cooperativaId") UUID cooperativaId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

    @Query(value = """
            SELECT fn_total_kg_por_categoria(
                :categoriaId,
                :dataInicio,
                :dataFim
            )
            """, nativeQuery = true)
    BigDecimal calcularTotalKgPorCategoria(
            @Param("categoriaId") UUID categoriaId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim);

    @Procedure(procedureName = "sp_aceitar_pedido_cooperativa")
    void aceitarPedidoCooperativa(
            @Param("p_pedido_cooperativa_id") UUID pedidoCooperativaId,
            @Param("p_status_aceito_id") UUID statusAceitoId);


    @Procedure(procedureName = "sp_fechar_negociacao")
    void fecharNegociacao(
            @Param("p_negociacao_id") UUID negociacaoId,
            @Param("p_aceito") Boolean aceito);


    @Procedure(procedureName = "sp_fechar_rateio_mensal")
    void fecharRateioMensal(
            @Param("p_cooperativa_id") UUID cooperativaId,
            @Param("p_gestor_id") UUID gestorId,
            @Param("p_mes_referencia") LocalDateTime mesReferencia);



    @Procedure(procedureName = "sp_registrar_movimentacao_triagem")
    void registrarMovimentacaoTriagem(
            @Param("p_equipe_id") UUID equipeId,
            @Param("p_coleta_id") UUID coletaId,
            @Param("p_material_id") UUID materialId,
            @Param("p_status_id") UUID statusId,
            @Param("p_quantidade_kg") BigDecimal quantidadeKg,
            @Param("p_quantidade_rejeito_kg") BigDecimal quantidadeRejeitoKg,
            @Param("p_data_triagem") LocalDateTime dataTriagem);

}