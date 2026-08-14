package com.renovai.api.controller;
 
import com.renovai.api.dto.request.LoginRequest;
import com.renovai.api.dto.request.Requests.EsqueciSenhaRequest;
import com.renovai.api.dto.request.Requests.RedefinirSenhaRequest;
import com.renovai.api.dto.response.Responses.LoginResponse;
import com.renovai.api.service.AuthService;
import com.renovai.api.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Login, logout e recuperação de senha")
public class AuthController {
 
    private final AuthService authService;
    private final UsuarioService usuarioService;
 
    public AuthController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
    }
 
    @PostMapping("/login")
    @Operation(summary = "Realizar login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
 
    @PostMapping("/logout")
    @Operation(summary = "Logout — invalida o token no cliente")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
 
    @PostMapping("/esqueci-senha")
    @Operation(summary = "Solicitar redefinição de senha — tela 1.7")
    public ResponseEntity<Void> esqueciSenha(@RequestBody @Valid EsqueciSenhaRequest request) {
        authService.solicitarRedefinicaoSenha(request.email());
        return ResponseEntity.noContent().build();
    }
 
    @PostMapping("/redefinir-senha")
    @Operation(summary = "Redefinir senha com token — tela 1.7.1")
    public ResponseEntity<Void> redefinirSenha(@RequestBody @Valid RedefinirSenhaRequest request) {
        authService.redefinirSenha(request.token(), request.novaSenha());
        return ResponseEntity.noContent().build();
    }
}