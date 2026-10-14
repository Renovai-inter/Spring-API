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

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final PerfilRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final EmpresaSchemaService empresaSchemaService;

    public AuthService(
            UsuarioRepository usuarioRepository,
            FuncionarioRepository funcionarioRepository,
            PerfilRepository perfilRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            EmpresaSchemaService empresaSchemaService) {
        this.usuarioRepository = usuarioRepository;
        this.funcionarioRepository = funcionarioRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.empresaSchemaService = empresaSchemaService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        if (empresaSchemaService.ehEmpresa(request.email())) {
            return empresaSchemaService.login(request);
        }

        if (perfilRepository.findByEmail(request.email())
                .filter(p -> p.getSenhaHash() != null).isPresent()) {
            return loginPerfil(request);
        }
        Optional<Usuario> usuarioEncontrado = usuarioRepository.findByEmail(request.email());
        if (usuarioEncontrado.isEmpty()) {
            return loginPerfil(request);
        }
        Usuario usuario = usuarioEncontrado.get();

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new RegraDeNegocioException("Credenciais inválidas.");
        }
        return respostaUsuario(usuario);
    }

    /** Monta o login de um funcionário já autenticado (por senha ou pelo Google). */
    private LoginResponse respostaUsuario(Usuario usuario) {
        Funcionario funcionario =
                funcionarioRepository
                        .findByUsuario(usuario)
                        .orElseThrow(
                                () -> new RegraDeNegocioException("Funcionário não encontrado."));

        if (!"ATIVO".equals(funcionario.getStatusFuncionario())) {
            throw new RegraDeNegocioException("Usuário inativo ou afastado.");
        }

        usuario.setUltimoAcesso(LocalDateTime.now());
        usuarioRepository.save(usuario);

        String role = roleDoCargo(funcionario.getCargo().getCargo());
        return new LoginResponse(
                tokenProvider.gerarToken(usuario.getEmail(), role),
                usuario.getEmail(),
                role,
                usuario.getUsuarioId());
    }

    private LoginResponse loginPerfil(LoginRequest request) {
        Perfil perfil =
                perfilRepository
                        .findByEmail(request.email())
                        .orElseThrow(() -> new RegraDeNegocioException("Credenciais inválidas."));
        if (!Boolean.TRUE.equals(perfil.getEstaAtivo())) {
            throw new RegraDeNegocioException("Perfil inativo.");
        }
        if (!passwordEncoder.matches(request.senha(), perfil.getSenhaHash())) {
            throw new RegraDeNegocioException("Credenciais inválidas.");
        }
        return respostaPerfil(perfil);
    }

    /** Monta o login de um perfil (gestor de empresa / admin de cooperativa) já autenticado. */
    private LoginResponse respostaPerfil(Perfil perfil) {
        String role = perfil.getEmpresa() != null ? "GESTOR_EMPRESA" : "ADMIN_COOPERATIVA";
        return new LoginResponse(
                tokenProvider.gerarToken(perfil.getEmail(), role),
                perfil.getEmail(),
                role,
                perfil.getPerfilId());
    }

    /**
     * Login de quem já teve o e-mail confirmado por outro provedor (ex.: Google). Segue a mesma
     * ordem de busca do login por senha e devolve o mesmo JWT; nunca cria conta nova.
     */
    @Transactional
    public LoginResponse loginVerificado(String email) {
        if (empresaSchemaService.ehEmpresa(email)) {
            return empresaSchemaService.loginVerificado(email);
        }
        Optional<Perfil> perfilComSenha =
                perfilRepository.findByEmailIgnoreCase(email).filter(p -> p.getSenhaHash() != null);
        if (perfilComSenha.isPresent()) {
            return respostaPerfilAtivo(perfilComSenha.get());
        }
        Optional<Usuario> usuario = usuarioRepository.findByEmailIgnoreCase(email);
        if (usuario.isPresent()) {
            return respostaUsuario(usuario.get());
        }
        return perfilRepository
                .findByEmailIgnoreCase(email)
                .map(this::respostaPerfilAtivo)
                .orElseThrow(
                        () ->
                                new RegraDeNegocioException(
                                        "Não existe conta no Renovaí com este e-mail. Faça o cadastro primeiro."));
    }

    private LoginResponse respostaPerfilAtivo(Perfil perfil) {
        if (!Boolean.TRUE.equals(perfil.getEstaAtivo())) {
            throw new RegraDeNegocioException("Perfil inativo.");
        }
        return respostaPerfil(perfil);
    }

    private String roleDoCargo(String cargo) {
        String role = cargo.trim().toUpperCase(Locale.ROOT).replace(" ", "_");
        if (role.startsWith("ROLE_")) {
            role = role.substring(5);
        }
        return switch (role) {
            case "GESTOR" -> "GESTOR_COOPERATIVA";
            case "ADMIN" -> "ADMIN_COOPERATIVA";
            case "ADMINISTRADOR" -> "ADMIN_SITE";
            default -> role;
        };
    }

    @Transactional
    public String solicitarRedefinicaoSenha(String email) {
        String token = gerarTokenSeguro();
        LocalDateTime expiracao = LocalDateTime.now().plusMinutes(30);
        Optional<Perfil> perfilEncontrado = perfilRepository.findByEmailIgnoreCase(email)
                .filter(p -> p.getSenhaHash() != null);
        Optional<Usuario> usuarioEncontrado =
                perfilEncontrado.isPresent()
                        ? Optional.empty()
                        : usuarioRepository.findByEmail(email);
        if (usuarioEncontrado.isPresent()) {
            Usuario usuario = usuarioEncontrado.get();
            usuario.setTokenRedefinicao(token);
            usuario.setTokenExpiracao(expiracao);
            usuarioRepository.save(usuario);
        } else {
            Perfil perfil =
                    perfilEncontrado.or(() -> perfilRepository.findByEmail(email)).orElseThrow(
                            () -> new RegraDeNegocioException("E-mail não encontrado."));
            perfil.setTokenRedefinicao(token);
            perfil.setTokenExpiracao(expiracao);
            perfilRepository.save(perfil);
        }

        return token;
    }

    @Transactional
    public void redefinirSenha(String token, String novaSenha) {
        if (novaSenha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RegraDeNegocioException("Senha deve ter até 72 bytes.");
        }
        Optional<Perfil> perfilEncontrado = perfilRepository.findByTokenRedefinicao(token);
        Optional<Usuario> usuarioEncontrado =
                perfilEncontrado.isPresent()
                        ? Optional.empty()
                        : usuarioRepository.findByTokenRedefinicao(token);
        if (usuarioEncontrado.isPresent()) {
            Usuario usuario = usuarioEncontrado.get();
            validarExpiracao(usuario.getTokenExpiracao());
            usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
            usuario.setTokenRedefinicao(null);
            usuario.setTokenExpiracao(null);
            usuarioRepository.save(usuario);
        } else {
            Perfil perfil =
                    perfilEncontrado.orElseThrow(
                            () -> new RegraDeNegocioException("Token inválido ou expirado."));
            validarExpiracao(perfil.getTokenExpiracao());
            perfil.setSenhaHash(passwordEncoder.encode(novaSenha));
            perfil.setTokenRedefinicao(null);
            perfil.setTokenExpiracao(null);
            perfilRepository.save(perfil);
        }
    }

    private void validarExpiracao(LocalDateTime expiracao) {
        if (expiracao == null || !LocalDateTime.now().isBefore(expiracao)) {
            throw new RegraDeNegocioException("Token expirado. Solicite uma nova redefinição.");
        }
    }

    private String gerarTokenSeguro() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
