package com.renovai.api.controller;
 
import com.renovai.api.dto.request.LoginRequest;
import com.renovai.api.dto.request.Requests.EsqueciSenhaRequest;
import com.renovai.api.dto.request.Requests.GoogleLoginRequest;
import com.renovai.api.dto.request.Requests.RedefinirSenhaRequest;
import com.renovai.api.dto.request.Requests.VerificarCodigoRequest;
import com.renovai.api.dto.response.Responses.LoginResponse;
import com.renovai.api.service.AuthService;
import com.renovai.api.service.CodigoRedefinicaoService;
import com.renovai.api.service.GoogleAuthService;
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
    private final CodigoRedefinicaoService codigoRedefinicaoService;
    private final GoogleAuthService googleAuthService;
 
    public AuthController(
            AuthService authService,
            UsuarioService usuarioService,
            CodigoRedefinicaoService codigoRedefinicaoService,
            GoogleAuthService googleAuthService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
        this.codigoRedefinicaoService = codigoRedefinicaoService;
        this.googleAuthService = googleAuthService;
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
 
    @PostMapping("/google")
    @Operation(summary = "Login com Google — recebe o ID token do Google e devolve o JWT do Renovaí")
    public ResponseEntity<LoginResponse> loginGoogle(@RequestBody @Valid GoogleLoginRequest request) {
        return ResponseEntity.ok(googleAuthService.login(request.idToken()));
    }
 
    @PostMapping("/esqueci-senha")
    @Operation(
            summary = "Solicitar redefinição de senha — tela 1.7",
            description = "Envia um código de 6 dígitos para o e-mail. Responde 204 mesmo se o e-mail não tiver conta.")
    public ResponseEntity<Void> esqueciSenha(@RequestBody @Valid EsqueciSenhaRequest request) {
        codigoRedefinicaoService.solicitar(request.email());
        return ResponseEntity.noContent().build();
    }
 
    @PostMapping("/verificar-codigo")
    @Operation(summary = "Conferir o código de redefinição sem gastá-lo")
    public ResponseEntity<Void> verificarCodigo(@RequestBody @Valid VerificarCodigoRequest request) {
        codigoRedefinicaoService.verificar(request.email(), request.codigo());
        return ResponseEntity.noContent().build();
    }
 
    @PostMapping("/redefinir-senha")
    @Operation(
            summary = "Redefinir senha — tela 1.7.1",
            description = "Com email: token é o código de 6 dígitos enviado por e-mail. Sem email: token antigo de redefinição.")
    public ResponseEntity<Void> redefinirSenha(@RequestBody @Valid RedefinirSenhaRequest request) {
        if (request.email() != null && !request.email().isBlank()) {
            codigoRedefinicaoService.redefinir(request.email(), request.token(), request.novaSenha());
        } else {
            authService.redefinirSenha(request.token(), request.novaSenha());
        }
        return ResponseEntity.noContent().build();
    }
}