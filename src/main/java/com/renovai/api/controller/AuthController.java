package com.renovai.api.controller;

import com.renovai.api.dto.request.LoginRequest;
import com.renovai.api.dto.response.Responses.LoginResponse;
import com.renovai.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Login e geração de token JWT")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Realizar login", description = "Autentica um perfil e retorna um token JWT.")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Client deve descartar o token. JWT é stateless.")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
