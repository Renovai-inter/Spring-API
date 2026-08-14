package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests;
import com.renovai.api.dto.response.Responses;
import com.renovai.api.service.FunctionEProceduresService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/functions")
@RequiredArgsConstructor
public class FunctionEProceduresController {

    private final FunctionEProceduresService service;


    /*
     * ============================================================
     * FUNCTIONS
     * ============================================================
     */

    @GetMapping("/avaliacoes/media/{perfilId}")
    public ResponseEntity<Responses.MediaAvaliacoesResponse> calcularMediaAvaliacoes(
            @PathVariable UUID perfilId
    ) {
        BigDecimal resultado = service.calcularMediaAvaliacoes(perfilId);

        return ResponseEntity.ok(
            new Responses.MediaAvaliacoesResponse(resultado)
        );
    }


    @GetMapping("/cooperativas/{cooperativaId}/cooperados")
    public ResponseEntity<Responses.NumeroCooperadosResponse> calcularNumeroCooperados(
            @PathVariable UUID cooperativaId
    ) {
        Integer resultado = service.calcularNumeroCooperados(cooperativaId);

        return ResponseEntity.ok(
            new Responses.NumeroCooperadosResponse(resultado)
        );
    }


    @PostMapping("/coletas/rejeito")
    public ResponseEntity<Responses.QuantidadeRejeitoResponse> calcularQuantidadeRejeitoKg(
            @RequestBody Requests.CalcularRejeitoRequest request
    ) {
        BigDecimal resultado = service.calcularQuantidadeRejeitoKg(
            request.triagemQuantidadeKg(),
            request.coletaId()
        );

        return ResponseEntity.ok(
            new Responses.QuantidadeRejeitoResponse(resultado)
        );
    }


    @PostMapping("/rateio/automatico")
    public ResponseEntity<Responses.RateioAutomaticoResponse> calcularRateioAutomatico(
            @RequestBody Requests.CalcularRateioRequest request
    ) {
        BigDecimal resultado = service.calcularRateioAutomatico(
            request.cooperativaId(),
            request.mesReferencia()
        );

        return ResponseEntity.ok(
            new Responses.RateioAutomaticoResponse(resultado)
        );
    }


    @PostMapping("/financeiro/acumulado")
    public ResponseEntity<Responses.TotalAcumuladoResponse> calcularTotalAcumulado(
            @RequestBody Requests.CalcularTotalRequest request
    ) {
        BigDecimal resultado = service.calcularTotalAcumulado(
            request.cooperativaId(),
            request.dataInicio(),
            request.dataFim()
        );

        return ResponseEntity.ok(
            new Responses.TotalAcumuladoResponse(resultado)
        );
    }


    @PostMapping("/financeiro/liquido")
    public ResponseEntity<Responses.TotalLiquidoResponse> calcularTotalLiquido(
            @RequestBody Requests.CalcularTotalRequest request
    ) {
        BigDecimal resultado = service.calcularTotalLiquido(
            request.cooperativaId(),
            request.dataInicio(),
            request.dataFim()
        );

        return ResponseEntity.ok(
            new Responses.TotalLiquidoResponse(resultado)
        );
    }


    @PostMapping("/categorias/total-kg")
    public ResponseEntity<Responses.TotalKgCategoriaResponse> calcularTotalKgPorCategoria(
            @RequestBody Requests.CalcularTotalCategoriaRequest request
    ) {
        BigDecimal resultado = service.calcularTotalKgPorCategoria(
            request.categoriaId(),
            request.dataInicio(),
            request.dataFim()
        );

        return ResponseEntity.ok(
            new Responses.TotalKgCategoriaResponse(resultado)
        );
    }


    /*
     * ============================================================
     * PROCEDURES
     * ============================================================
     */

    @PostMapping("/pedidos/aceitar")
    public ResponseEntity<Responses.ProcedureResponse> aceitarPedidoCooperativa(
            @RequestBody Requests.AceitarPedidoCooperativaRequest request
    ) {
        service.aceitarPedidoCooperativa(
            request.pedidoCooperativaId(),
            request.statusAceitoId()
        );

        return ResponseEntity.ok(
            new Responses.ProcedureResponse(
                true,
                "Pedido da cooperativa aceito com sucesso."
            )
        );
    }


    @PostMapping("/negociacoes/fechar")
    public ResponseEntity<Responses.ProcedureResponse> fecharNegociacao(
            @RequestBody Requests.FecharNegociacaoProcedureRequest request
    ) {
        service.fecharNegociacao(
            request.negociacaoId(),
            request.aceito()
        );

        return ResponseEntity.ok(
            new Responses.ProcedureResponse(
                true,
                "Negociação fechada com sucesso."
            )
        );
    }


    @PostMapping("/rateios/fechar")
    public ResponseEntity<Responses.ProcedureResponse> fecharRateioMensal(
            @RequestBody Requests.FecharRateioMensalRequest request
    ) {
        service.fecharRateioMensal(
            request.cooperativaId(),
            request.gestorId(),
            request.mesReferencia()
        );

        return ResponseEntity.ok(
            new Responses.ProcedureResponse(
                true,
                "Rateio mensal fechado com sucesso."
            )
        );
    }


    @PostMapping("/triagens/movimentacao")
    public ResponseEntity<Responses.ProcedureResponse> registrarMovimentacaoTriagem(
            @RequestBody Requests.RegistrarMovimentacaoTriagemRequest request
    ) {
        service.registrarMovimentacaoTriagem(
            request.equipeId(),
            request.coletaId(),
            request.materialId(),
            request.statusId(),
            request.quantidadeKg(),
            request.quantidadeRejeitoKg(),
            request.dataTriagem()
        );

        return ResponseEntity.ok(
            new Responses.ProcedureResponse(
                true,
                "Movimentação da triagem registrada com sucesso."
            )
        );
    }
}