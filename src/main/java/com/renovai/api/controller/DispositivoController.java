package com.renovai.api.controller;

import com.renovai.api.service.NotificacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

/** Aparelhos que recebem push; ficam no Firestore (ver NotificacaoService), não no banco. */
@RestController
@RequestMapping("/dispositivos")
@Tag(name = "Dispositivos", description = "Aparelhos que recebem as notificações push do app")
public class DispositivoController {

    /** Token do FCM; sem "/" porque vira o id do documento no Firestore. */
    public record DispositivoRequest(
            @NotBlank @Size(max = 1024) @Pattern(regexp = "[^/]+") String token) {}

    private final NotificacaoService notificacoes;

    public DispositivoController(NotificacaoService notificacoes) {
        this.notificacoes = notificacoes;
    }

    @PostMapping
    @Operation(summary = "Registrar o aparelho para a conta logada (substitui a conta anterior do aparelho)")
    public ResponseEntity<Void> registrar(@RequestBody @Valid DispositivoRequest request, Principal principal) {
        notificacoes.registrarDispositivo(request.token(), principal.getName());
        return ResponseEntity.noContent().build();
    }

    /** Diagnóstico do ambiente: sem acesso ao cluster, diz se o FIREBASE_CREDENTIALS foi carregado. */
    @GetMapping("/status")
    @Operation(summary = "Informar se as notificações push estão ligadas neste servidor")
    public Map<String, Boolean> status() {
        return Map.of("pushAtivo", notificacoes.ativo());
    }

    @DeleteMapping
    @Operation(summary = "Descadastrar o aparelho (logout)")
    public ResponseEntity<Void> remover(@RequestParam String token, Principal principal) {
        if (!token.isBlank() && !token.contains("/"))
            notificacoes.removerDispositivo(token, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
