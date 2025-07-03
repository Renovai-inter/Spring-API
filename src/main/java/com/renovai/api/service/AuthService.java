package com.renovai.api.service;

import com.renovai.api.dto.request.LoginRequest;
import com.renovai.api.dto.response.Responses.LoginResponse;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.Perfil;
import com.renovai.api.repository.PerfilRepository;
import com.renovai.api.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(PerfilRepository perfilRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider) {
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public LoginResponse login(LoginRequest request) {
        Perfil perfil = perfilRepository.findByEmail(request.email())
                .orElseThrow(() -> new RegraDeNegocioException("Credenciais inválidas."));

        if (!perfil.getEstaAtivo()) {
            throw new RegraDeNegocioException("Perfil inativo. Entre em contato com o administrador.");
        }

        // Determina o role baseado no tipo de perfil
        String role = determinarRole(perfil);
        String token = tokenProvider.gerarToken(perfil.getEmail(), role);

        return new LoginResponse(token, perfil.getEmail(), role);
    }

    private String determinarRole(Perfil perfil) {
        if (perfil.getCooperativa() != null) {
            return "GESTOR_COOPERATIVA";
        } else if (perfil.getEmpresa() != null) {
            return "GESTOR_EMPRESA";
        }
        return "ADMIN_SITE";
    }
}
