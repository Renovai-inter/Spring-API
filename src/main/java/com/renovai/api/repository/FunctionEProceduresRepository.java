package com.renovai.api.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public class FunctionEProceduresRepository {

    @PersistenceContext
    private EntityManager em;

    // ========== FUNCTIONS ==========

    public BigDecimal calcularMediaAvaliacoes(UUID perfilId) {
        Query q = em.createNativeQuery("SELECT fn_calcular_media_avaliacoes(:perfilId)");
        q.setParameter("perfilId", perfilId);
        return (BigDecimal) q.getSingleResult();
    }

    public Integer calcularNumeroCooperados(UUID cooperativaId) {
        Query q = em.createNativeQuery("SELECT fn_calcular_numero_cooperados(:cooperativaId)");
        q.setParameter("cooperativaId", cooperativaId);
        return ((Number) q.getSingleResult()).intValue();
    }

    public BigDecimal calcularQuantidadeRejeitoKg(BigDecimal triagemQuantidadeKg, UUID coletaId) {
        Query q = em.createNativeQuery("SELECT fn_calcular_quantidade_rejeito_kg(:triagemQuantidadeKg, :coletaId)");
        q.setParameter("triagemQuantidadeKg", triagemQuantidadeKg);
        q.setParameter("coletaId", coletaId);
        return (BigDecimal) q.getSingleResult();
    }

    public BigDecimal calcularRateioAutomatico(UUID cooperativaId, LocalDateTime mesReferencia) {
        Query q = em.createNativeQuery("SELECT fn_calcular_rateio_automatico(:cooperativaId, :mesReferencia)");
        q.setParameter("cooperativaId", cooperativaId);
        q.setParameter("mesReferencia", mesReferencia);
        return (BigDecimal) q.getSingleResult();
    }

    public BigDecimal calcularTotalAcumulado(UUID cooperativaId, LocalDateTime dataInicio, LocalDateTime dataFim) {
        Query q = em.createNativeQuery("SELECT fn_calcular_total_acumulado(:cooperativaId, :dataInicio, :dataFim)");
        q.setParameter("cooperativaId", cooperativaId);
        q.setParameter("dataInicio", dataInicio);
        q.setParameter("dataFim", dataFim);
        return (BigDecimal) q.getSingleResult();
    }

    public BigDecimal calcularTotalLiquido(UUID cooperativaId, LocalDateTime dataInicio, LocalDateTime dataFim) {
        Query q = em.createNativeQuery("SELECT fn_calcular_total_liquido(:cooperativaId, :dataInicio, :dataFim)");
        q.setParameter("cooperativaId", cooperativaId);
        q.setParameter("dataInicio", dataInicio);
        q.setParameter("dataFim", dataFim);
        return (BigDecimal) q.getSingleResult();
    }

    public BigDecimal calcularTotalKgPorCategoria(UUID categoriaId, LocalDateTime dataInicio, LocalDateTime dataFim) {
        Query q = em.createNativeQuery("SELECT fn_total_kg_por_categoria(:categoriaId, :dataInicio, :dataFim)");
        q.setParameter("categoriaId", categoriaId);
        q.setParameter("dataInicio", dataInicio);
        q.setParameter("dataFim", dataFim);
        return (BigDecimal) q.getSingleResult();
    }

    // ========== PROCEDURES ==========

    @Transactional
    public void aceitarPedidoCooperativa(UUID pedidoCooperativaId, UUID statusAceitoId) {
        Query q = em.createNativeQuery("CALL sp_aceitar_pedido_cooperativa(:p_pedido_cooperativa_id, :p_status_aceito_id)");
        q.setParameter("p_pedido_cooperativa_id", pedidoCooperativaId);
        q.setParameter("p_status_aceito_id", statusAceitoId);
        q.executeUpdate();
    }

    @Transactional
    public void fecharNegociacao(UUID negociacaoId, Boolean aceito) {
        Query q = em.createNativeQuery("CALL sp_fechar_negociacao(:p_negociacao_id, :p_aceito)");
        q.setParameter("p_negociacao_id", negociacaoId);
        q.setParameter("p_aceito", aceito);
        q.executeUpdate();
    }

    @Transactional
    public void fecharRateioMensal(UUID cooperativaId, UUID gestorId, LocalDateTime mesReferencia) {
        Query q = em.createNativeQuery("CALL sp_fechar_rateio_mensal(:p_cooperativa_id, :p_gestor_id, :p_mes_referencia)");
        q.setParameter("p_cooperativa_id", cooperativaId);
        q.setParameter("p_gestor_id", gestorId);
        q.setParameter("p_mes_referencia", mesReferencia);
        q.executeUpdate();
    }

    @Transactional
    public void registrarMovimentacaoTriagem(
            UUID equipeId, UUID coletaId, UUID materialId, UUID statusId,
            BigDecimal quantidadeKg, BigDecimal quantidadeRejeitoKg, LocalDateTime dataTriagem) {
        Query q = em.createNativeQuery(
            "CALL sp_registrar_movimentacao_triagem(:p_equipe_id, :p_coleta_id, :p_material_id, :p_status_id, :p_quantidade_kg, :p_quantidade_rejeito_kg, :p_data_triagem)");
        q.setParameter("p_equipe_id", equipeId);
        q.setParameter("p_coleta_id", coletaId);
        q.setParameter("p_material_id", materialId);
        q.setParameter("p_status_id", statusId);
        q.setParameter("p_quantidade_kg", quantidadeKg);
        q.setParameter("p_quantidade_rejeito_kg", quantidadeRejeitoKg);
        q.setParameter("p_data_triagem", dataTriagem);
        q.executeUpdate();
    }
}