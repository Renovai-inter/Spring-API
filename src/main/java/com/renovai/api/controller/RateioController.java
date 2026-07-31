package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.RateioGeralRequest;
import com.renovai.api.dto.request.Requests.RateioProporcionalsRequest;
import com.renovai.api.dto.response.Responses.RateioRealizadoResponse;
import com.renovai.api.dto.response.Responses.RateioListaResponse;
import com.renovai.api.dto.response.Responses.RateioDetalheResponse;
import com.renovai.api.dto.response.Responses.RateioFuncionarioResponse;
import com.renovai.api.service.RateioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/rateios")
@Tag(name = "Rateios", description = "Sistema de rateio de lucros para cooperados")
public class RateioController {
    private final RateioService service;
    public RateioController(RateioService service) { this.service = service; }

    @PostMapping("/executar-geral")
    public ResponseEntity<RateioRealizadoResponse> executarRateioGeral(
            @RequestBody @Valid RateioGeralRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.executarRateioGeral(request));
    }

    @PostMapping("/executar-proporcional")
    public ResponseEntity<RateioRealizadoResponse> executarRateioProporcional(
            @RequestBody @Valid RateioProporcionalsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.executarRateioProporcional(request));
    }

    @GetMapping("/listar/{cooperativaId}")
    public ResponseEntity<List<RateioListaResponse>> listar(
            @PathVariable UUID cooperativaId) {
        return ResponseEntity.ok(service.listarTodos(cooperativaId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RateioDetalheResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @GetMapping("/por-cooperativa/{cooperativaId}")
    public ResponseEntity<List<RateioListaResponse>> listarPorCooperativa(
            @PathVariable UUID cooperativaId) {
        return ResponseEntity.ok(service.listarPorCooperativa(cooperativaId));
    }

    @GetMapping("/{id}/distribuicao")
    public ResponseEntity<List<RateioFuncionarioResponse>> listarDistribuicao(
            @PathVariable UUID id) {
        return ResponseEntity.ok(service.listarDistribuicaoPorRateio(id));
    }
}