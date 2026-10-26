package com.renovai.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.renovai.api.exception.RegraDeNegocioException;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

class GoogleAuthServiceTest {
    private static final Set<String> CLIENTES = Set.of("web-client.apps.googleusercontent.com");

    @Test
    void tokenDoAppComEmailVerificadoDevolveOEmail() {
        Map<String, Object> dados =
                Map.of(
                        "aud", "web-client.apps.googleusercontent.com",
                        "iss", "https://accounts.google.com",
                        "email", "empresa@renovai.com",
                        "email_verified", "true");

        assertThat(GoogleAuthService.emailVerificado(dados, CLIENTES)).isEqualTo("empresa@renovai.com");
    }

    @Test
    void tokenDeOutroAppERecusado() {
        Map<String, Object> dados =
                Map.of(
                        "aud", "outro-app.apps.googleusercontent.com",
                        "iss", "accounts.google.com",
                        "email", "empresa@renovai.com",
                        "email_verified", "true");

        assertThatThrownBy(() -> GoogleAuthService.emailVerificado(dados, CLIENTES))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("Este login do Google não pertence ao app Renovaí.");
    }

    @Test
    void emailNaoVerificadoERecusado() {
        Map<String, Object> dados =
                Map.of(
                        "aud", "web-client.apps.googleusercontent.com",
                        "iss", "accounts.google.com",
                        "email", "empresa@renovai.com",
                        "email_verified", "false");

        assertThatThrownBy(() -> GoogleAuthService.emailVerificado(dados, CLIENTES))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("O e-mail desta conta Google não está verificado.");
    }

    @Test
    void semClientIdConfiguradoNemConsultaOGoogle() {
        AuthService authService = mock(AuthService.class);
        GoogleAuthService service = new GoogleAuthService(authService, "");

        assertThatThrownBy(() -> service.login("qualquer-token"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessage("O login com Google não está configurado no servidor.");
        verifyNoInteractions(authService);
    }
}
