package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.PreCadastroRequest;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.*;
import com.renovai.api.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 
    public record FuncionarioRequest(UUID usuarioId, UUID cargoId, UUID cooperativaId) {}
 
    public record FuncionarioResponse(
            UUID funcionarioId, UUID usuarioId, String usuarioNome,
            UUID cargoId, String cargo,
            UUID cooperativaId, String cooperativaNome,
            Boolean estaAtivo) {}
 

    public record PreCadastroIncompletoResponse(
            UUID funcionarioId,
            UUID usuarioId,
            String usuarioNome,
            String cpf,
            UUID cooperativaId,
            String cooperativaNome,
            String cargoAtribuido,
            Boolean temEmailCompleto,
            java.time.LocalDateTime dataAdmissao) {}
 
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
    public List<FuncionarioResponse> listarPorCooperativa(UUID cooperativaId) {
        if (!cooperativaRepository.existsById(cooperativaId)) {
            throw new RecursoNaoEncontradoException("Cooperativa", cooperativaId);
        }
        return repository.findByCooperativa_CooperativaId(cooperativaId).stream().map(this::toResponse).toList();
    }
 
    /**
     * Busca todos os funcionários que fizeram pré-cadastro (0.3) mas não completaram o cadastro (0.4)
     * Identifica usuários com CPF e senha temporária, mas sem email definido
     * 
     * @return Lista de funcionários com pré-cadastro incompleto em toda a plataforma
     */
    @Transactional(readOnly = true)
    public List<PreCadastroIncompletoResponse> listarComPreCadastroIncompleto() {
        return repository.findComPreCadastroIncompleto()
                .stream()
                .map(this::toPreCadastroIncompletoResponse)
                .toList();
    }
 
    /**
     * Busca funcionários com pré-cadastro incompleto de uma cooperativa específica
     * 
     * @param cooperativaId ID da cooperativa
     * @return Lista de funcionários com pré-cadastro incompleto nessa cooperativa
     */
    @Transactional(readOnly = true)
    public List<PreCadastroIncompletoResponse> listarComPreCadastroIncompletoByCooperativa(UUID cooperativaId) {
        if (!cooperativaRepository.existsById(cooperativaId)) {
            throw new RecursoNaoEncontradoException("Cooperativa", cooperativaId);
        }
        return repository.findComPreCadastroIncompletoByCooperativa(cooperativaId)
                .stream()
                .map(this::toPreCadastroIncompletoResponse)
                .toList();
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
                .usuario(usuario).cargo(cargo).cooperativa(cooperativa).estaAtivo(true).build();
        return toResponse(repository.save(funcionario));
    }
 
    public FuncionarioResponse atualizarCargo(UUID id, UUID cargoId) {
        Funcionario funcionario = findOrThrow(id);
        Cargo cargo = cargoRepository.findById(cargoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cargo", cargoId));
        funcionario.setCargo(cargo);
        return toResponse(repository.save(funcionario));
    }
 
    /**
     * Realiza o pré-cadastro de um funcionário (Tela 0.3 do fluxo Renovaí)
     * Cria usuário com CPF e senha temporária, email ainda não preenchido
     * 
     * @param request Dados do pré-cadastro (nome, cpf, senhaTemporaria, cargoId, cooperativaId)
     * @return Response do funcionário criado
     */
    public FuncionarioResponse preCadastro(PreCadastroRequest request) {
        if (usuarioRepository.findByCpf(request.cpf()).isPresent()) {
            throw new RuntimeException("CPF já cadastrado.");
        }
 
        Cargo cargo = cargoRepository.findById(request.cargoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cargo", request.cargoId()));
        Cooperativa cooperativa = cooperativaRepository.findById(request.cooperativaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", request.cooperativaId()));
 
        // email é NOT NULL no banco — usa string vazia como placeholder até completarCadastro()
        Usuario usuario = Usuario.builder()
                .nome(request.nome())
                .cpf(request.cpf())
                .email("")
                .senhaHash(passwordEncoder.encode(request.senhaTemporaria()))
                .build();
        usuario = usuarioRepository.save(usuario);
 
        Funcionario funcionario = Funcionario.builder()
                .usuario(usuario).cargo(cargo).cooperativa(cooperativa).estaAtivo(true).build();
        funcionario = repository.save(funcionario);
 
        return toResponse(funcionario);
    }
 
    public FuncionarioResponse desativar(UUID id) {
        Funcionario funcionario = findOrThrow(id);
        funcionario.setEstaAtivo(false);
        return toResponse(repository.save(funcionario));
    }
 
    public void deletar(UUID id) {
        findOrThrow(id);
        repository.deleteById(id);
    }
 
    private Funcionario findOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Funcionário", id));
    }
 
    public Funcionario findEntityById(UUID id) {
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
 
    /**
     * Converte Funcionario para response de pré-cadastro incompleto
     */
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