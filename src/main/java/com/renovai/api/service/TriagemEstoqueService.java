package com.renovai.api.service;

import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.Triagem;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class TriagemEstoqueService {
    private final JdbcTemplate jdbc;

    public TriagemEstoqueService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void bloquearColeta(UUID coletaId) {
        List<UUID> coletas =
                jdbc.query(
                        "select evento_id from coletas where evento_id=? for update",
                        (r, i) -> r.getObject(1, UUID.class),
                        coletaId);
        if (coletas.isEmpty()) throw new RecursoNaoEncontradoException("Coleta", coletaId);
    }

    public UUID bloquearColetaDaTriagem(UUID triagemId) {
        List<UUID> coletas =
                jdbc.query(
                        "select coleta_id from triagens where evento_id=?",
                        (r, i) -> r.getObject(1, UUID.class),
                        triagemId);
        if (coletas.isEmpty()) throw new RecursoNaoEncontradoException("Triagem", triagemId);
        bloquearColeta(coletas.get(0));
        return coletas.get(0);
    }

    public boolean temMovimentacoes(UUID triagemId) {
        return jdbc.queryForObject(
                        "select count(*) from movimentacoes_estoques where triagem_id=?",
                        Long.class,
                        triagemId)
                > 0;
    }

    public void sincronizar(Triagem triagem) {
        BigDecimal quantidade = concluida(triagem) ? triagem.getQuantidadeKg() : BigDecimal.ZERO;
        UUID cooperativaId = triagem.getEquipe().getGestor().getCooperativa().getCooperativaId();
        UUID materialId = triagem.getMaterial().getMaterialId();
        jdbc.queryForObject(
                "select cooperativa_id from cooperativas where cooperativa_id=? for update",
                UUID.class,
                cooperativaId);
        List<UUID> estoques =
                jdbc.query(
                        "select estoque_id from estoques where cooperativa_id=? and material_id=?"
                                + " for update",
                        (r, i) -> r.getObject(1, UUID.class),
                        cooperativaId,
                        materialId);
        BigDecimal registrado =
                jdbc.queryForObject(
                        "select coalesce(sum(quantidade_kg),0) from movimentacoes_estoques where"
                                + " triagem_id=?",
                        BigDecimal.class,
                        triagem.getEventoId());
        BigDecimal diferenca = quantidade.subtract(registrado);
        if (diferenca.signum() == 0) return;
        UUID estoqueId;
        if (estoques.isEmpty()) {
            estoqueId = UUID.randomUUID();
            jdbc.update(
                    "insert into estoques(estoque_id,cooperativa_id,material_id,quantidade_kg)"
                            + " values(?,?,?,0)",
                    estoqueId,
                    cooperativaId,
                    materialId);
        } else {
            estoqueId = estoques.get(0);
        }
        BigDecimal antes =
                jdbc.queryForObject(
                        "select quantidade_kg from estoques where estoque_id=? for update",
                        BigDecimal.class,
                        estoqueId);
        BigDecimal esperado = antes.add(diferenca);
        if (esperado.signum() < 0)
            throw new RegraDeNegocioException("Estoque insuficiente para corrigir a triagem.");
        jdbc.update(
                "insert into"
                    + " movimentacoes_estoques(estoque_id,triagem_id,quantidade_kg,tipo_movimentacao)"
                    + " values(?,?,?,?)",
                estoqueId,
                triagem.getEventoId(),
                diferenca,
                diferenca.signum() > 0 ? "ENTRADA" : "SAIDA");
        BigDecimal depois =
                jdbc.queryForObject(
                        "select quantidade_kg from estoques where estoque_id=?",
                        BigDecimal.class,
                        estoqueId);
        if (depois.compareTo(antes) == 0) {
            jdbc.update(
                    "update estoques set quantidade_kg=?,data_atualizacao=now() where estoque_id=?",
                    esperado,
                    estoqueId);
        } else if (depois.compareTo(esperado) != 0) {
            throw new RegraDeNegocioException(
                    "Saldo divergente após registrar a movimentação da triagem.");
        }
    }

    public boolean concluida(Triagem triagem) {
        return triagem.getStatus() != null
                && Set.of("CONCLUIDO", "CONCLUIDA", "FINALIZADO")
                        .contains(
                                NegociacaoFluxoService.normalizar(
                                        triagem.getStatus().getStatusAtual()));
    }
}
