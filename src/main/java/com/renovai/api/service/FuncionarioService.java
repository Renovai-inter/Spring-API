package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.PreCadastroRequest;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.Cargo;
import com.renovai.api.model.Cooperativa;
import com.renovai.api.model.Funcionario;
import com.renovai.api.model.Usuario;
import com.renovai.api.repository.CargoRepository;
import com.renovai.api.repository.CooperativaRepository;
import com.renovai.api.repository.FuncionarioRepository;
import com.renovai.api.repository.UsuarioRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class FuncionarioService {

    private final FuncionarioRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final CargoRepository cargoRepository;
    private final CooperativaRepository cooperativaRepository;
    private final PasswordEncoder passwordEncoder;

    public FuncionarioService(
        FuncionarioRepository repository,
        UsuarioRepository usuarioRepository,
        CargoRepository cargoRepository,
        CooperativaRepository cooperativaRepository,
        PasswordEncoder passwordEncoder) {

        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.cargoRepository = cargoRepository;
        this.cooperativaRepository = cooperativaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record FuncionarioRequest(Integer usuarioId, Integer cargoId, Integer cooperativaId) {}

    public record FuncionarioResponse(
            Integer funcionarioId, Integer usuarioId, String usuarioNome,
            Integer cargoId, String cargo,
            Integer cooperativaId, String cooperativaNome,
            Boolean estaAtivo) {}

    @Transactional(readOnly = true)
    public List<FuncionarioResponse> listarAtivos() {
        return repository.findByEstaAtivoTrue().stream().map(this::toResponse).toList();
    }

    public List<FuncionarioResponse> listarPorCargo(String cargo) {
        return repository.findAtivosByCargo(cargo.toUpperCase()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FuncionarioResponse> listarTodos() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FuncionarioResponse> listarPorCooperativa(Integer cooperativaId) {
        if (!cooperativaRepository.existsById(cooperativaId)) {
            throw new RecursoNaoEncontradoException("Cooperativa", cooperativaId);
        }
        return repository.findByCooperativa_CooperativaId(cooperativaId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public FuncionarioResponse buscarPorId(Integer id) {
        return toResponse(findOrThrow(id));
    }

    public FuncionarioResponse criar(FuncionarioRequest request) {
        Usuario usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário", request.usuarioId()));
        Cargo cargo = cargoRepository.findById(request.cargoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cargo", request.cargoId()));
        Cooperativa cooperativa = cooperativaRepository.findById(request.cooperativaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", request.cooperativaId()));

        Funcionario funcionario = Funcionario.builder()
                .usuario(usuario)
                .cargo(cargo)
                .cooperativa(cooperativa)
                .estaAtivo(true)
                .build();
        return toResponse(repository.save(funcionario));
    }

    public FuncionarioResponse atualizarCargo(Integer id, Integer cargoId) {
        Funcionario funcionario = findOrThrow(id);
        Cargo cargo = cargoRepository.findById(cargoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cargo", cargoId));
        funcionario.setCargo(cargo);
        return toResponse(repository.save(funcionario));
    }

    public FuncionarioResponse preCadastro(PreCadastroRequest request) {

        if (usuarioRepository.findByCpf(request.cpf()).isPresent()) {
            throw new RuntimeException("CPF já cadastrado.");
        }

        Cargo cargo = cargoRepository.findById(request.cargoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cargo", request.cargoId()));

        Cooperativa cooperativa = cooperativaRepository.findById(request.cooperativaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", request.cooperativaId()));

        Usuario usuario = Usuario.builder()
                .nome(request.nome())
                .cpf(request.cpf())
                .email(null)
                .senhaHash(passwordEncoder.encode(request.senhaTemporaria()))
                .build();

        usuario = usuarioRepository.save(usuario);

        Funcionario funcionario = Funcionario.builder()
                .usuario(usuario)
                .cargo(cargo)
                .cooperativa(cooperativa)
                .estaAtivo(true)
                .build();

        funcionario = repository.save(funcionario);

        return toResponse(funcionario);
    }

    public FuncionarioResponse desativar(Integer id) {
        Funcionario funcionario = findOrThrow(id);
        funcionario.setEstaAtivo(false);
        return toResponse(repository.save(funcionario));
    }

    public void deletar(Integer id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    private Funcionario findOrThrow(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário", id));
    }

    public Funcionario findEntityById(Integer id) {
        return findOrThrow(id);
    }

    private FuncionarioResponse toResponse(Funcionario f) {
        return new FuncionarioResponse(
                f.getFuncionarioId(),
                f.getUsuario().getUsuarioId(),
                f.getUsuario().getNome(),
                f.getCargo().getCargoId(),
                f.getCargo().getCargo(),
                f.getCooperativa().getCooperativaId(),
                f.getCooperativa().getNome(),
                f.getEstaAtivo()
        );
    }
}