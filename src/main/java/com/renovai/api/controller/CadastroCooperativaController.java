package com.renovai.api.controller;

import com.renovai.api.dto.request.CadastroCooperativaRequest;
import com.renovai.api.dto.response.CadastroCooperativaResponse;
import com.renovai.api.service.CadastroCooperativaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Autenticação")
public class CadastroCooperativaController {
    private final CadastroCooperativaService service;

    public CadastroCooperativaController(CadastroCooperativaService service) {
        this.service = service;
    }

    @PostMapping("/auth/cadastro-cooperativa")
    @SecurityRequirements
    @Operation(
            summary = "Cadastrar cooperativa, endereço e administrador",
            description =
                    "Cadastro público e transacional. Após o cadastro, faça login com e-mail e"
                        + " senha.")
    public ResponseEntity<CadastroCooperativaResponse> cadastrar(
            @RequestBody @Valid CadastroCooperativaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrar(request));
    }
}
