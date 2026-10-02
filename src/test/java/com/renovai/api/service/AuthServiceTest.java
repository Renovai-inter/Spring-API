package com.renovai.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.renovai.api.dto.request.LoginRequest;
import com.renovai.api.dto.response.Responses.LoginResponse;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.Cargo;
import com.renovai.api.model.Cooperativa;
import com.renovai.api.model.Funcionario;
import com.renovai.api.model.Perfil;
import com.renovai.api.model.Usuario;
import com.renovai.api.repository.FuncionarioRepository;
import com.renovai.api.repository.PerfilRepository;
import com.renovai.api.repository.UsuarioRepository;
import com.renovai.api.security.JwtTokenProvider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private FuncionarioRepository funcionarioRepository;
    @Mock private PerfilRepository perfilRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;
    @Mock private EmpresaSchemaService empresaSchemaService;

    private AuthService service;
    private final LoginRequest login = new LoginRequest("usuario@renovai.com", "senha123");

    @BeforeEach
    void preparar() {
        service =
                new AuthService(
                        usuarioRepository,
                        funcionarioRepository,
                        perfilRepository,
                        passwordEncoder,
                        tokenProvider,
                        empresaSchemaService);
    }

    @Test
    void empresaUsaFluxoCompativelSemConsultarEntidadesJpa() {
        LoginResponse esperado =
                new LoginResponse("token", login.email(), "GESTOR_EMPRESA", UUID.randomUUID());
        when(empresaSchemaService.ehEmpresa(login.email())).thenReturn(true);
        when(empresaSchemaService.login(login)).thenReturn(esperado);

        assertThat(service.login(login)).isSameAs(esperado);
        verifyNoInteractions(usuarioRepository, funcionarioRepository, perfilRepository);
    }

    @ParameterizedTest
    @CsvSource({
        "Gestor,GESTOR_COOPERATIVA",
        "Cooperado,COOPERADO",
        "Administrador,ADMIN_SITE",
        "Admin,ADMIN_SITE",
        "Motorista,MOTORISTA",
        "ROLE_GESTOR_COOPERATIVA,GESTOR_COOPERATIVA"
    })
    void loginRetornaIdTipoBearerECargoCompativelComPermissoes(String cargoNome, String role) {
        Usuario usuario = usuario();
        Cargo cargo = new Cargo();
        cargo.setCargo(cargoNome);
        Funcionario funcionario = new Funcionario();
        funcionario.setCargo(cargo);
        funcionario.setStatusFuncionario("ATIVO");
        when(usuarioRepository.findByEmail(login.email())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(login.senha(), usuario.getSenhaHash())).thenReturn(true);
        when(funcionarioRepository.findByUsuario(usuario)).thenReturn(Optional.of(funcionario));
        when(tokenProvider.gerarToken(login.email(), role)).thenReturn("token");

        LoginResponse response = service.login(login);

        assertThat(response.usuarioId()).isEqualTo(usuario.getUsuarioId());
        assertThat(response.tipo()).isEqualTo("Bearer");
        assertThat(response.role()).isEqualTo(role);
        assertThat(response.token()).isEqualTo("token");
        assertThat(usuario.getUltimoAcesso()).isNotNull();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void senhaIncorretaNaoEmiteTokenNemAtualizaUltimoAcesso() {
        Usuario usuario = usuario();
        when(usuarioRepository.findByEmail(login.email())).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.login(login))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Credenciais inválidas.");
        verifyNoInteractions(tokenProvider, funcionarioRepository);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void funcionarioInativoNaoPodeEntrar() {
        Usuario usuario = usuario();
        Funcionario funcionario = new Funcionario();
        funcionario.setStatusFuncionario("INATIVO");
        when(usuarioRepository.findByEmail(login.email())).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches(login.senha(), usuario.getSenhaHash())).thenReturn(true);
        when(funcionarioRepository.findByUsuario(usuario)).thenReturn(Optional.of(funcionario));

        assertThatThrownBy(() -> service.login(login)).hasMessage("Usuário inativo ou afastado.");
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void preservaLoginDoAdministradorDaCooperativaPorPerfil() {
        Perfil perfil = perfil();
        perfil.setCooperativa(new Cooperativa());
        when(perfilRepository.findByEmail(login.email())).thenReturn(Optional.of(perfil));
        when(passwordEncoder.matches(login.senha(), perfil.getSenhaHash())).thenReturn(true);

        LoginResponse response = service.login(login);

        assertThat(response.role()).isEqualTo("ADMIN_COOPERATIVA");
        assertThat(response.usuarioId()).isEqualTo(perfil.getPerfilId());
        verify(tokenProvider).gerarToken(login.email(), "ADMIN_COOPERATIVA");
    }

    @Test
    void perfilInativoNaoPodeEntrar() {
        Perfil perfil = perfil();
        perfil.setEstaAtivo(false);
        when(perfilRepository.findByEmail(login.email())).thenReturn(Optional.of(perfil));

        assertThatThrownBy(() -> service.login(login)).hasMessage("Perfil inativo.");
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void geraTokenSeguroComExpiracaoDeTrintaMinutosParaUsuario() {
        Usuario usuario = usuario();
        when(usuarioRepository.findByEmail(login.email())).thenReturn(Optional.of(usuario));
        LocalDateTime antes = LocalDateTime.now();

        String token = service.solicitarRedefinicaoSenha(login.email());

        assertThat(token).matches("[A-Za-z0-9_-]{43}");
        assertThat(usuario.getTokenRedefinicao()).isEqualTo(token);
        assertThat(usuario.getTokenExpiracao())
                .isBetween(antes.plusMinutes(30), LocalDateTime.now().plusMinutes(30));
        verify(usuarioRepository).save(usuario);
        verify(perfilRepository).findByEmailIgnoreCase(login.email());
    }

    @Test
    void preservaRecuperacaoDeSenhaDoPerfilInstitucional() {
        Perfil perfil = perfil();
        when(perfilRepository.findByEmail(login.email())).thenReturn(Optional.of(perfil));

        String token = service.solicitarRedefinicaoSenha(login.email());

        assertThat(perfil.getTokenRedefinicao()).isEqualTo(token);
        verify(perfilRepository).save(perfil);
    }

    @Test
    void redefineSenhaDoUsuarioEConsomeToken() {
        Usuario usuario = usuario();
        usuario.setTokenRedefinicao("token");
        usuario.setTokenExpiracao(LocalDateTime.now().plusMinutes(5));
        when(usuarioRepository.findByTokenRedefinicao("token")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("novaSenha")).thenReturn("novoHash");

        service.redefinirSenha("token", "novaSenha");

        assertThat(usuario.getSenhaHash()).isEqualTo("novoHash");
        assertThat(usuario.getTokenRedefinicao()).isNull();
        assertThat(usuario.getTokenExpiracao()).isNull();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void redefineSenhaDoPerfilEConsomeToken() {
        Perfil perfil = perfil();
        perfil.setTokenExpiracao(LocalDateTime.now().plusMinutes(5));
        when(perfilRepository.findByTokenRedefinicao("token")).thenReturn(Optional.of(perfil));
        when(passwordEncoder.encode("novaSenha")).thenReturn("novoHash");

        service.redefinirSenha("token", "novaSenha");

        assertThat(perfil.getSenhaHash()).isEqualTo("novoHash");
        assertThat(perfil.getTokenRedefinicao()).isNull();
        assertThat(perfil.getTokenExpiracao()).isNull();
        verify(perfilRepository).save(perfil);
    }

    @Test
    void tokenExpiradoNaoAlteraSenha() {
        Usuario usuario = usuario();
        usuario.setTokenExpiracao(LocalDateTime.now().minusSeconds(1));
        when(usuarioRepository.findByTokenRedefinicao("token")).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.redefinirSenha("token", "novaSenha"))
                .hasMessage("Token expirado. Solicite uma nova redefinição.");
        verifyNoInteractions(passwordEncoder);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void senhaAcimaDoLimiteEmBytesDoBcryptNaoEhPersistida() {
        assertThatThrownBy(() -> service.redefinirSenha("token", "á".repeat(37)))
                .hasMessage("Senha deve ter até 72 bytes.");
        verifyNoInteractions(usuarioRepository, perfilRepository, passwordEncoder);
    }

    @Test
    void cooperativaPriorizaCredencialInstitucionalMesmoComUsuarioDoMesmoEmail() {
        Perfil perfil = perfil();
        perfil.setCooperativa(new Cooperativa());
        when(perfilRepository.findByEmail(login.email())).thenReturn(Optional.of(perfil));
        when(passwordEncoder.matches(login.senha(), perfil.getSenhaHash())).thenReturn(true);
        assertThat(service.login(login).usuarioId()).isEqualTo(perfil.getPerfilId());
        verifyNoInteractions(usuarioRepository, funcionarioRepository);
    }

    @Test
    void recuperacaoPriorizaMesmaContaInstitucionalDoLogin() {
        Perfil perfil = perfil();
        perfil.setEmpresa(new com.renovai.api.model.Empresa());
        when(perfilRepository.findByEmailIgnoreCase(login.email())).thenReturn(Optional.of(perfil));
        service.solicitarRedefinicaoSenha(login.email());
        verify(perfilRepository).save(perfil);
        verifyNoInteractions(usuarioRepository);
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setUsuarioId(UUID.randomUUID());
        usuario.setEmail(login.email());
        usuario.setSenhaHash("hash");
        return usuario;
    }

    private Perfil perfil() {
        Perfil perfil = new Perfil();
        perfil.setPerfilId(UUID.randomUUID());
        perfil.setEmail(login.email());
        perfil.setSenhaHash("hash");
        perfil.setEstaAtivo(true);
        return perfil;
    }
}
