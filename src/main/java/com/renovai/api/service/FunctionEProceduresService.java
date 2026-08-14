package com.renovai.api.service;

import com.renovai.api.repository.FunctionEProceduresRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FunctionEProceduresService {

    private final FunctionEProceduresRepository repository;

    @Transactional(readOnly = true)
    public BigDecimal calcularMediaAvaliacoes(UUID perfilId) {
        return repository.calcularMediaAvaliacoes(perfilId);
    }

    @Transactional(readOnly = true)
    public Integer calcularNumeroCooperados(UUID cooperativaId) {
        return repository.calcularNumeroCooperados(cooperativaId);
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularQuantidadeRejeitoKg(
            BigDecimal triagemQuantidadeKg,
            UUID coletaId
    ) {
        return repository.calcularQuantidadeRejeitoKg(
                triagemQuantidadeKg,
                coletaId
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularRateioAutomatico(
            UUID cooperativaId,
            LocalDateTime mesReferencia
    ) {
        return repository.calcularRateioAutomatico(
                cooperativaId,
                mesReferencia
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularTotalAcumulado(
            UUID cooperativaId,
            LocalDateTime dataInicio,
            LocalDateTime dataFim
    ) {
        return repository.calcularTotalAcumulado(
                cooperativaId,
                dataInicio,
                dataFim
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularTotalLiquido(
            UUID cooperativaId,
            LocalDateTime dataInicio,
            LocalDateTime dataFim
    ) {
        return repository.calcularTotalLiquido(
                cooperativaId,
                dataInicio,
                dataFim
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularTotalKgPorCategoria(
            UUID categoriaId,
            LocalDateTime dataInicio,
            LocalDateTime dataFim
    ) {
        return repository.calcularTotalKgPorCategoria(
                categoriaId,
                dataInicio,
                dataFim
        );
    }


    /*
     * ============================================================
     * PROCEDURES
     * ============================================================
     */

    @Transactional
    public void aceitarPedidoCooperativa(
            UUID pedidoCooperativaId,
            UUID statusAceitoId
    ) {
        repository.aceitarPedidoCooperativa(
                pedidoCooperativaId,
                statusAceitoId
        );
    }

    @Transactional
    public void fecharNegociacao(
            UUID negociacaoId,
            Boolean aceito
    ) {
        repository.fecharNegociacao(
                negociacaoId,
                aceito
        );
    }

    @Transactional
    public void fecharRateioMensal(
            UUID cooperativaId,
            UUID gestorId,
            LocalDateTime mesReferencia
    ) {
        repository.fecharRateioMensal(
                cooperativaId,
                gestorId,
                mesReferencia
        );
    }

    @Transactional
    public void registrarMovimentacaoTriagem(
            UUID equipeId,
            UUID coletaId,
            UUID materialId,
            UUID statusId,
            BigDecimal quantidadeKg,
            BigDecimal quantidadeRejeitoKg,
            LocalDateTime dataTriagem
    ) {
        repository.registrarMovimentacaoTriagem(
                equipeId,
                coletaId,
                materialId,
                statusId,
                quantidadeKg,
                quantidadeRejeitoKg,
                dataTriagem
        );
    }
}