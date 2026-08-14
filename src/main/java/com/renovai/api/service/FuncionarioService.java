package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.PreCadastroRequest;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.*;
import com.renovai.api.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class FuncionarioService {

    private final FuncionarioRepository repository;
    private final UsuarioRepository usuarioRepository;
    private final CargoRepository cargoRepository;
    private final CooperativaRepository cooperativaRepository;
    private final PasswordEncoder passwordEncoder;

    public FuncionarioService(FuncionarioRepository repository,
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

    public record FuncionarioRequest(UUID usuarioId, UUID cargoId, UUID cooperativaId) {}

    public record FuncionarioResponse(
            UUID funcionarioId, UUID usuarioId, String usuarioNome,
            UUID cargoId, String cargo,
            UUID cooperativaId, String cooperativaNome,
            Boolean estaAtivo, String statusFuncionario) {}

    public record PreCadastroIncompletoResponse(
            UUID funcionarioId,
            UUID usuarioId,
            String usuarioNome,
            String cpf,
            UUID cooperativaId,
            String cooperativaNome,
            String cargoAtribuido,
            Boolean temEmailCompleto,
            LocalDateTime dataAdmissao) {}

    @Transactional(readOnly = true)
    public List<FuncionarioResponse> listarAtivos() {
        return repository.findAtivos().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FuncionarioResponse> listarPorCargo(String cargo) {
        return repository.findAtivosByCargo(cargo.toUpperCase()).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FuncionarioResponse> listarTodos() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FuncionarioResponse> listarPorCooperativa(UUID cooperativaId) {
        if (!cooperativaRepository.existsById(cooperativaId))
            throw new RecursoNaoEncontradoException("Cooperativa", cooperativaId);
        return repository.findByCooperativa_CooperativaId(cooperativaId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FuncionarioResponse> listarMotoristasPorCooperativa(UUID cooperativaId) {
        if (!cooperativaRepository.existsById(cooperativaId))
            throw new RecursoNaoEncontradoException("Cooperativa", cooperativaId);
        return repository.findMotoristasByCooperativa(cooperativaId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<FuncionarioResponse> listarPorCooperativaEStatus(UUID cooperativaId, String status) {
        return repository.findByCooperativaAndStatus(cooperativaId, status).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PreCadastroIncompletoResponse> listarComPreCadastroIncompleto() {
        return repository.findComPreCadastroIncompleto().stream()
                .map(this::toPreCadastroIncompletoResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PreCadastroIncompletoResponse> listarComPreCadastroIncompletoByCooperativa(UUID cooperativaId) {
        if (!cooperativaRepository.existsById(cooperativaId))
            throw new RecursoNaoEncontradoException("Cooperativa", cooperativaId);
        return repository.findComPreCadastroIncompletoByCooperativa(cooperativaId).stream()
                .map(this::toPreCadastroIncompletoResponse).toList();
    }

    @Transactional(readOnly = true)
    public FuncionarioResponse buscarPorId(UUID id) {
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
                .statusFuncionario("ATIVO")
                .build();
        return toResponse(repository.save(funcionario));
    }

    public FuncionarioResponse atualizarCargo(UUID id, UUID cargoId) {
        Funcionario funcionario = findOrThrow(id);
        Cargo cargo = cargoRepository.findById(cargoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cargo", cargoId));
        funcionario.setCargo(cargo);
        return toResponse(repository.save(funcionario));
    }

    public FuncionarioResponse preCadastro(PreCadastroRequest request) {
        if (usuarioRepository.findByCpf(request.cpf()).isPresent())
            throw new RegraDeNegocioException("CPF já cadastrado.");

        Cargo cargo = cargoRepository.findById(request.cargoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cargo", request.cargoId()));
        Cooperativa cooperativa = cooperativaRepository.findById(request.cooperativaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", request.cooperativaId()));

        Usuario usuario = Usuario.builder()
                .nome(request.nome())
                .cpf(request.cpf())
                .email("")
                .senhaHash(passwordEncoder.encode(request.senhaTemporaria()))
                .build();
        usuario = usuarioRepository.save(usuario);

        Funcionario funcionario = Funcionario.builder()
                .usuario(usuario)
                .cargo(cargo)
                .cooperativa(cooperativa)
                .statusFuncionario("ATIVO")
                .build();
        return toResponse(repository.save(funcionario));
    }

    public FuncionarioResponse desativar(UUID id) {
        Funcionario funcionario = findOrThrow(id);
        funcionario.setStatusFuncionario("INATIVO");
        funcionario.setDataDesligamento(LocalDateTime.now());
        return toResponse(repository.save(funcionario));
    }

    public FuncionarioResponse afastar(UUID id) {
        Funcionario funcionario = findOrThrow(id);
        funcionario.setStatusFuncionario("AFASTADO");
        return toResponse(repository.save(funcionario));
    }

    public FuncionarioResponse reativar(UUID id) {
        Funcionario funcionario = findOrThrow(id);
        funcionario.setStatusFuncionario("ATIVO");
        funcionario.setDataDesligamento(null);
        return toResponse(repository.save(funcionario));
    }

    public void deletar(UUID id) {
        findOrThrow(id);
        repository.deleteById(id);
    }

    public Funcionario findEntityById(UUID id) {
        return findOrThrow(id);
    }

    private Funcionario findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário", id));
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
                f.getEstaAtivo(),
                f.getStatusFuncionario()
        );
    }

    private PreCadastroIncompletoResponse toPreCadastroIncompletoResponse(Funcionario f) {
        Usuario usuario = f.getUsuario();
        boolean temEmailCompleto = usuario.getEmail() != null && !usuario.getEmail().trim().isEmpty();
        return new PreCadastroIncompletoResponse(
                f.getFuncionarioId(),
                usuario.getUsuarioId(),
                usuario.getNome(),
                usuario.getCpf(),
                f.getCooperativa().getCooperativaId(),
                f.getCooperativa().getNome(),
                f.getCargo().getCargo(),
                temEmailCompleto,
                f.getDataAdmissao()
        );
    }
}