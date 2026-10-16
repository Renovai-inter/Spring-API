package com.renovai.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.Perfil;
import com.renovai.api.model.Usuario;
import com.renovai.api.repository.PerfilRepository;
import com.renovai.api.repository.UsuarioRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class CodigoRedefinicaoServiceTest {
    private static final String EMAIL = "cooperado@renovai.com";

    @Mock private PerfilRepository perfilRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    private CodigoRedefinicaoService service;

    @BeforeEach
    void preparar() {
        service =
                new CodigoRedefinicaoService(
                        perfilRepository, usuarioRepository, passwordEncoder, emailService);
    }

    @Test
    void solicitarGuardaSoOHashEEnviaOCodigoPorEmail() {
        Usuario usuario = usuario();
        semPerfil();
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));
        when(emailService.configurado()).thenReturn(true);
        when(emailService.modelo(eq("codigo-redefinicao.html"), anyMap())).thenReturn("<html/>");

        service.solicitar(" Cooperado@Renovai.com ");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> valores = ArgumentCaptor.forClass(Map.class);
        verify(emailService).modelo(eq("codigo-redefinicao.html"), valores.capture());
        String codigo = valores.getValue().get("CODIGO");
        assertThat(codigo).matches("\\d{6}");
        assertThat(usuario.getTokenRedefinicao())
                .isEqualTo(CodigoRedefinicaoService.hash(EMAIL, codigo))
                .isNotEqualTo(codigo);
        assertThat(usuario.getTokenExpiracao()).isAfter(LocalDateTime.now().plusMinutes(14));
        verify(emailService).enviar(eq(EMAIL), anyString(), eq("<html/>"), anyString());
    }

    @Test
    void semEmailConfiguradoGuardaOCodigoMasNaoFalha() {
        Usuario usuario = usuario();
        semPerfil();
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));
        when(emailService.configurado()).thenReturn(false);

        service.solicitar(EMAIL);

        assertThat(usuario.getTokenRedefinicao()).isNotBlank();
        verify(emailService, never()).enviar(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void emailSemContaNaoEnviaNadaNemAvisa() {
        semPerfil();
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        service.solicitar(EMAIL);

        verify(emailService, never()).enviar(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void codigoCertoTrocaASenhaEInvalidaOCodigo() {
        Usuario usuario = usuarioComCodigo("123456", LocalDateTime.now().plusMinutes(10));
        semPerfil();
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("novaSenha1")).thenReturn("novo-hash");

        service.redefinir(EMAIL, "123456", "novaSenha1");

        assertThat(usuario.getSenhaHash()).isEqualTo("novo-hash");
        assertThat(usuario.getTokenRedefinicao()).isNull();
        assertThat(usuario.getTokenExpiracao()).isNull();
    }

    @Test
    void codigoErradoAvisaQuantasTentativasRestam() {
        Usuario usuario = usuarioComCodigo("123456", LocalDateTime.now().plusMinutes(10));
        semPerfil();
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.verificar(EMAIL, "000000"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Código incorreto. Restam 4 tentativas.");
        assertThat(usuario.getTokenRedefinicao()).isNotNull();
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void cincoErrosInvalidamOCodigo() {
        Usuario usuario = usuarioComCodigo("123456", LocalDateTime.now().plusMinutes(10));
        semPerfil();
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));

        for (int i = 1; i < CodigoRedefinicaoService.MAX_TENTATIVAS; i++) {
            assertThatThrownBy(() -> service.verificar(EMAIL, "000000"))
                    .isInstanceOf(RegraDeNegocioException.class);
        }
        assertThatThrownBy(() -> service.verificar(EMAIL, "000000"))
                .hasMessage("Muitas tentativas erradas. Solicite um novo código.");
        assertThat(usuario.getTokenRedefinicao()).isNull();
    }

    @Test
    void codigoExpiradoNaoTrocaASenha() {
        Usuario usuario = usuarioComCodigo("123456", LocalDateTime.now().minusSeconds(1));
        semPerfil();
        when(usuarioRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.redefinir(EMAIL, "123456", "novaSenha1"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Código expirado. Solicite um novo código.");
        assertThat(usuario.getSenhaHash()).isEqualTo("hash");
    }

    @Test
    void perfilComSenhaTemPrioridadeComoNoLogin() {
        Perfil perfil = new Perfil();
        perfil.setPerfilId(UUID.randomUUID());
        perfil.setEmail(EMAIL);
        perfil.setSenhaHash("hash-perfil");
        perfil.setTokenRedefinicao(CodigoRedefinicaoService.hash(EMAIL, "654321"));
        perfil.setTokenExpiracao(LocalDateTime.now().plusMinutes(10));
        when(perfilRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(perfil));
        when(passwordEncoder.encode("novaSenha1")).thenReturn("novo-hash");

        service.redefinir(EMAIL, "654321", "novaSenha1");

        assertThat(perfil.getSenhaHash()).isEqualTo("novo-hash");
        assertThat(perfil.getTokenRedefinicao()).isNull();
        verify(usuarioRepository, never()).findByEmailIgnoreCase(any());
    }

    private void semPerfil() {
        when(perfilRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setUsuarioId(UUID.randomUUID());
        usuario.setEmail(EMAIL);
        usuario.setSenhaHash("hash");
        return usuario;
    }

    private Usuario usuarioComCodigo(String codigo, LocalDateTime expiracao) {
        Usuario usuario = usuario();
        usuario.setTokenRedefinicao(CodigoRedefinicaoService.hash(EMAIL, codigo));
        usuario.setTokenExpiracao(expiracao);
        return usuario;
    }
}
