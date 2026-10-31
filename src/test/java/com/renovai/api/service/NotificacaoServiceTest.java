package com.renovai.api.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseOptions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class NotificacaoServiceTest {

    @Test
    void inicializaOpcoesFirebaseComDependenciasDeRuntime() {
        // Reproduz o build que falhava no deploy, sem credenciais reais ou acesso à rede.
        FirebaseOptions opcoes = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.create(new AccessToken("token-de-teste", null)))
                .build();

        assertThat(opcoes.getJsonFactory().getClass().getName())
                .isEqualTo("com.google.api.client.json.jackson2.JacksonFactory");
    }

    @Test
    void semCredenciaisOPushFicaDesligadoSemDerrubarAApi() {
        assertThat(new NotificacaoService(null, "").ativo()).isFalse();
    }

    @Test
    void credenciaisInvalidasDesligamOPushSemDerrubarAApi() {
        assertThat(new NotificacaoService(null, "{\"type\": \"service_account\"}").ativo()).isFalse();
        assertThat(new NotificacaoService(null, "/caminho/que/nao/existe.json").ativo()).isFalse();
    }

    @Test
    void naoAvisaQuemFezAAcao() {
        assertThat(NotificacaoService.semAutor(
                        List.of("Gestor@Coop.com", "outro@coop.com", " "), "gestor@coop.com"))
                .containsExactly("outro@coop.com");
    }

    @Test
    void estoqueAvisaSoNaQuedaAbaixoDe1000Kg() {
        UUID caiu = UUID.randomUUID(), jaBaixo = UUID.randomUUID(), reposto = UUID.randomUUID(),
                novo = UUID.randomUUID(), limite = UUID.randomUUID();
        Map<UUID, BigDecimal> antes = Map.of(
                caiu, new BigDecimal("1200"),
                jaBaixo, new BigDecimal("900"),
                reposto, new BigDecimal("800"),
                limite, new BigDecimal("1000"));
        Map<UUID, BigDecimal> depois = Map.of(
                caiu, new BigDecimal("999.5"),
                jaBaixo, new BigDecimal("700"),
                reposto, new BigDecimal("1500"),
                novo, new BigDecimal("10"),
                limite, new BigDecimal("1000"));
        assertThat(NotificacaoService.caiuAbaixoDoLimite(antes, depois)).containsExactly(caiu);
    }
}
