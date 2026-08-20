package com.renovai.api.service;

import com.renovai.api.dto.request.LoginRequest;
import com.renovai.api.dto.response.Responses.LoginResponse;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.Funcionario;
import com.renovai.api.model.Usuario;
import com.renovai.api.repository.FuncionarioRepository;
import com.renovai.api.repository.UsuarioRepository;
import com.renovai.api.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UsuarioRepository usuarioRepository,
            FuncionarioRepository funcionarioRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider) {
        this.usuarioRepository = usuarioRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new RegraDeNegocioException("Credenciais inválidas."));

        String senhaHasheada = hashSha256(request.senha());
        if (!senhaHasheada.equals(usuario.getSenhaHash()))
            throw new RegraDeNegocioException("Credenciais inválidas.");

        Funcionario funcionario = funcionarioRepository.findByUsuario(usuario)
                .orElseThrow(() -> new RegraDeNegocioException("Funcionário não encontrado."));

        if (!"ATIVO".equals(funcionario.getStatusFuncionario()))
            throw new RegraDeNegocioException("Usuário inativo ou afastado.");

        usuario.setUltimoAcesso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        String role = funcionario.getCargo().getCargo();
        return new LoginResponse(tokenProvider.gerarToken(usuario.getEmail(), role), usuario.getEmail(), role);
    }

    private String hashSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erro ao hashear senha.", e);
        }
    }

    @Transactional
    public String solicitarRedefinicaoSenha(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RegraDeNegocioException("E-mail não encontrado."));

        String token = gerarTokenSeguro();
        usuario.setTokenRedefinicao(token);
        usuario.setTokenExpiracao(LocalDateTime.now().plusMinutes(30));
        usuarioRepository.save(usuario);

        return token;
    }

    @Transactional
    public void redefinirSenha(String token, String novaSenha) {
        Usuario usuario = usuarioRepository.findByTokenRedefinicao(token)
                .orElseThrow(() -> new RegraDeNegocioException("Token inválido ou expirado."));

        if (usuario.getTokenExpiracao() == null
                || LocalDateTime.now().isAfter(usuario.getTokenExpiracao())) {
            throw new RegraDeNegocioException("Token expirado. Solicite uma nova redefinição.");
        }

        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuario.setTokenRedefinicao(null);
        usuario.setTokenExpiracao(null);
        usuarioRepository.save(usuario);
    }

    private String gerarTokenSeguro() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}