package com.renovai.api.controller;

import com.renovai.api.dto.response.Responses.CooperativaResponse;
import com.renovai.api.model.Funcionario;
import com.renovai.api.repository.FuncionarioRepository;
import com.renovai.api.service.CooperativaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.security.Principal;
import java.util.Locale;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gestor/cooperativa")
@PreAuthorize("hasAnyRole('GESTOR_COOPERATIVA', 'ADMIN_COOPERATIVA')")
public class GestorCooperativaController {

    private final FuncionarioRepository funcionarioRepository;
    private final CooperativaService cooperativaService;

    public GestorCooperativaController(
            FuncionarioRepository funcionarioRepository, CooperativaService cooperativaService) {
        this.funcionarioRepository = funcionarioRepository;
        this.cooperativaService = cooperativaService;
    }

    @GetMapping
    public ResponseEntity<CooperativaResponse> minhaCooperativa(Principal principal) {
        Funcionario gestor = funcionarioGestor(principal);
        return ResponseEntity.ok(cooperativaService.buscarPorId(gestor.getCooperativa().getCooperativaId()));
    }

    @PatchMapping("/foto")
    public ResponseEntity<CooperativaResponse> atualizarFoto(
            Principal principal, @RequestBody @Valid AtualizarFotoRequest request) {
        Funcionario gestor = funcionarioGestor(principal);
        if (!urlCloudinaryValida(request.imagemUrl())) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(cooperativaService.atualizarImagem(
                gestor.getCooperativa().getCooperativaId(), request.imagemUrl()));
    }

    private Funcionario funcionarioGestor(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new org.springframework.security.access.AccessDeniedException("Gestor não autenticado.");
        }
        return funcionarioRepository.findByUsuario_EmailIgnoreCaseAndEstaAtivoTrue(principal.getName())
                .stream()
                .filter(f -> f.getCargo() != null && f.getCargo().getCargo() != null)
                .filter(f -> {
                    String cargo = f.getCargo().getCargo().toUpperCase(Locale.ROOT);
                    return cargo.equals("GESTOR_COOPERATIVA") || cargo.equals("ADMIN_COOPERATIVA");
                })
                .findFirst()
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "Apenas o gestor da cooperativa pode consultar ou alterar sua foto."));
    }

    private boolean urlCloudinaryValida(String imagemUrl) {
        try {
            URI uri = URI.create(imagemUrl);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && "res.cloudinary.com".equalsIgnoreCase(uri.getHost());
        } catch (RuntimeException e) {
            return false;
        }
    }

    public record AtualizarFotoRequest(@NotBlank @Size(max = 2048) String imagemUrl) {}
}
