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

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash()))
            throw new RegraDeNegocioException("Credenciais inválidas.");

        Funcionario funcionario = funcionarioRepository.findByUsuario(usuario)
                .orElseThrow(() -> new RegraDeNegocioException("Funcionário não encontrado."));

        if (!funcionario.getEstaAtivo())
            throw new RegraDeNegocioException("Usuário inativo.");

        String role = funcionario.getCargo().getCargo();
        return new LoginResponse(tokenProvider.gerarToken(usuario.getEmail(), role), usuario.getEmail(), role);
    }
}