package com.renovai.api.controller;

import com.renovai.api.dto.request.Requests.RateioRequest;
import com.renovai.api.dto.response.Responses.RateioResponse;
import com.renovai.api.service.RateioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rateios")
@Tag(name = "Rateios", description = "Módulo financeiro — distribuição de ganhos entre cooperados")
public class RateioController {

    private final RateioService service;

    public RateioController(RateioService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_COOPERATIVA')")
    @Operation(summary = "Listar rateios")
    public ResponseEntity<List<RateioResponse>> listar() {
        return ResponseEntity.ok(service.listarTodos());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_COOPERATIVA')")
    @Operation(summary = "Buscar rateio por ID")
    public ResponseEntity<RateioResponse> buscarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_SITE','GESTOR_COOPERATIVA')")
    @Operation(summary = "Executar rateio",
               description = "Cria o rateio e aciona automaticamente a procedure `calcular_rateio` no banco.")
    public ResponseEntity<RateioResponse> criar(@RequestBody @Valid RateioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.criar(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_SITE')")
    @Operation(summary = "Excluir rateio")
    public ResponseEntity<Void> deletar(@PathVariable Integer id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
