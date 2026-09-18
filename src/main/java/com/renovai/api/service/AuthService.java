package com.renovai.api.service;

import com.renovai.api.dto.request.LoginRequest;
import com.renovai.api.dto.response.Responses.LoginResponse;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.Funcionario;
import com.renovai.api.model.Perfil;
import com.renovai.api.model.Usuario;
import com.renovai.api.repository.FuncionarioRepository;
import com.renovai.api.repository.PerfilRepository;
import com.renovai.api.repository.UsuarioRepository;
import com.renovai.api.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UsuarioRepository usuarioRepository,
            FuncionarioRepository funcionarioRepository, PerfilRepository perfilRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider) {
        this.usuarioRepository = usuarioRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }


    @Transactional
    public String solicitarRedefinicaoSenha(String email) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            String token = gerarTokenSeguro();
            usuario.setTokenRedefinicao(token);
            usuario.setTokenExpiracao(LocalDateTime.now().plusMinutes(30));
            usuarioRepository.save(usuario);
            return token;
        }

        Perfil perfil = perfilRepository.findByEmail(email)
                .orElseThrow(() -> new RegraDeNegocioException("E-mail não encontrado."));
        String token = gerarTokenSeguro();
        perfil.setTokenRedefinicao(token);
        perfil.setTokenExpiracao(LocalDateTime.now().plusMinutes(30));
        perfilRepository.save(perfil);
        return token;
    }

    @Transactional
    public void redefinirSenha(String token, String novaSenha) {
        Optional<Usuario> usuarioOpt = usuarioRepository.findByTokenRedefinicao(token);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (usuario.getTokenExpiracao() == null || LocalDateTime.now().isAfter(usuario.getTokenExpiracao())) {
                throw new RegraDeNegocioException("Token expirado. Solicite uma nova redefinição.");
            }
            usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
            usuario.setTokenRedefinicao(null);
            usuario.setTokenExpiracao(null);
            usuarioRepository.save(usuario);
            return;
        }

        Perfil perfil = perfilRepository.findByTokenRedefinicao(token)
                .orElseThrow(() -> new RegraDeNegocioException("Token inválido ou expirado."));
        if (perfil.getTokenExpiracao() == null || LocalDateTime.now().isAfter(perfil.getTokenExpiracao())) {
            throw new RegraDeNegocioException("Token expirado. Solicite uma nova redefinição.");
        }
        perfil.setSenhaHash(passwordEncoder.encode(novaSenha));
        perfil.setTokenRedefinicao(null);
        perfil.setTokenExpiracao(null);
        perfilRepository.save(perfil);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {

        Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(request.email());
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            if (passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
                Funcionario funcionario = funcionarioRepository.findByUsuario(usuario)
                        .orElseThrow(() -> new RegraDeNegocioException("Funcionário não encontrado."));
                if (!"ATIVO".equals(funcionario.getStatusFuncionario())) {
                    throw new RegraDeNegocioException("Usuário inativo ou afastado.");
                }
                usuario.setUltimoAcesso(LocalDateTime.now());
                usuarioRepository.save(usuario);
                String role = funcionario.getCargo().getCargo();
                return new LoginResponse(tokenProvider.gerarToken(usuario.getEmail(), role), usuario.getEmail(), role, usuario.getUsuarioId());
            }
        }

        Perfil perfil = perfilRepository.findByEmail(request.email())
                .orElseThrow(() -> new RegraDeNegocioException("Credenciais inválidas."));

        if (!perfil.getEstaAtivo()) {
            throw new RegraDeNegocioException("Perfil inativo.");
        }

        if (!passwordEncoder.matches(request.senha(), perfil.getSenhaHash())) {
            throw new RegraDeNegocioException("Credenciais inválidas.");
        }

        String role = perfil.getEmpresa() != null ? "GESTOR_EMPRESA" : "ADMIN_COOPERATIVA";
        return new LoginResponse(tokenProvider.gerarToken(perfil.getEmail(), role), perfil.getEmail(), role, perfil.getPerfilId());
    }


    private String gerarTokenSeguro() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
