package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.*;
import com.renovai.api.dto.response.Responses.*;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.Usuario;
import com.renovai.api.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarTodos() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(UUID id) {
        return toResponse(findOrThrow(id));
    }

    public UsuarioResponse criar(UsuarioRequest request) {
        if (request.cpf() != null && repository.existsByCpf(request.cpf())) {
            throw new RegraDeNegocioException("CPF já cadastrado no sistema.");
        }
        if (request.email() != null && repository.existsByEmail(request.email())) {
            throw new RegraDeNegocioException("E-mail já cadastrado no sistema.");
        }
        Usuario usuario = Usuario.builder()
                .nome(request.nome())
                .cpf(request.cpf())
                .email(request.email() != null ? request.email() : "")
                .senhaHash(passwordEncoder.encode(request.senha()))
                .dataNascimento(request.dataNascimento())
                .build();
        return toResponse(repository.save(usuario));
    }

    public UsuarioResponse completarCadastro(CompletarCadastroRequest request) {
        Usuario usuario = repository.findByCpf(request.cpf())
                .orElseThrow(() -> new RegraDeNegocioException("CPF não encontrado."));
        if (usuario.getEmail() != null && !usuario.getEmail().isBlank()) {
            throw new RegraDeNegocioException("Cadastro já concluído.");
        }
        usuario.setEmail(request.email());
        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        repository.save(usuario);
        return toResponse(usuario);
    }

    @Transactional(readOnly = true)
    public PrimeiroAcessoResponse validarPrimeiroAcesso(ValidarPrimeiroAcessoRequest request) {
        Usuario usuario = repository.findByCpf(request.cpf())
                .orElseThrow(() -> new RegraDeNegocioException("CPF não encontrado."));
        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new RegraDeNegocioException("Senha inválida.");
        }
        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            return new PrimeiroAcessoResponse(true, "Complete seu cadastro.");
        }
        return new PrimeiroAcessoResponse(false, "Usuário já cadastrado. Faça login.");
    }

    public UsuarioResponse atualizar(UUID id, UsuarioRequest request) {
        Usuario usuario = findOrThrow(id);
        usuario.setNome(request.nome());
        usuario.setDataNascimento(request.dataNascimento());
        if (request.senha() != null && !request.senha().isBlank()) {
            usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        }
        return toResponse(repository.save(usuario));
    }

    public void deletar(UUID id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    private Usuario findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", id));
    }

    public Usuario findEntityById(UUID id) {
        return findOrThrow(id);
    }

    private UsuarioResponse toResponse(Usuario u) {
        return new UsuarioResponse(u.getUsuarioId(), u.getNome(), u.getCpf(), u.getDataNascimento());
    }

    public void alterarSenha(UUID id, AlterarSenhaRequest request) {
        Usuario usuario = findOrThrow(id);
        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenhaHash())) {
            throw new RegraDeNegocioException("Senha atual incorreta.");
        }
        usuario.setSenhaHash(passwordEncoder.encode(request.novaSenha()));
        repository.save(usuario);
    }
}