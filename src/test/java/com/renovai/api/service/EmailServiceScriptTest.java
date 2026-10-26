package com.renovai.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.renovai.api.exception.RegraDeNegocioException;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

class EmailServiceScriptTest {

    private HttpServer servidor;

    @AfterEach
    void parar() {
        if (servidor != null) servidor.stop(0);
    }

    /** Sobe um "Apps Script" falso que guarda o corpo recebido e responde com o JSON dado. */
    private String scriptFalso(String respostaJson, AtomicReference<String> recebido) throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext(
                "/exec",
                troca -> {
                    recebido.set(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                    byte[] corpo = respostaJson.getBytes(StandardCharsets.UTF_8);
                    troca.getResponseHeaders().add("Content-Type", "application/json");
                    troca.sendResponseHeaders(200, corpo.length);
                    try (OutputStream out = troca.getResponseBody()) {
                        out.write(corpo);
                    }
                });
        servidor.start();
        return "http://127.0.0.1:" + servidor.getAddress().getPort() + "/exec";
    }

    private EmailService comScript(String url) {
        // Sem senha de SMTP: só o script está configurado.
        return new EmailService("", "", "smtp.gmail.com", 587, "Renovaí", url);
    }

    @Test
    void enviaSoDestinatarioCodigoEValidadeParaOScript() throws Exception {
        AtomicReference<String> recebido = new AtomicReference<>();
        EmailService service = comScript(scriptFalso("{\"ok\":true}", recebido));

        assertThat(service.configurado()).isTrue();
        service.enviarCodigo("pessoa@exemplo.com", "123456", 15);

        assertThat(recebido.get())
                .isEqualTo("{\"para\":\"pessoa@exemplo.com\",\"codigo\":\"123456\",\"validade\":\"15\"}");
    }

    @Test
    void scriptRecusandoViraErroAmigavel() throws Exception {
        EmailService service =
                comScript(scriptFalso("{\"ok\":false,\"erro\":\"segredo inválido\"}", new AtomicReference<>()));

        assertThatThrownBy(() -> service.enviarCodigo("pessoa@exemplo.com", "123456", 15))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Não foi possível enviar o e-mail");
    }

    @Test
    void semScriptNemSmtpNaoEstaConfigurado() {
        EmailService service = new EmailService("", "", "smtp.gmail.com", 587, "Renovaí", "-");
        assertThat(service.configurado()).isFalse();
    }

    @Test
    void scriptVemLigadoPorPadrao() {
        EmailService service =
                new EmailService("", "", "smtp.gmail.com", 587, "Renovaí", EmailService.SCRIPT_PADRAO);
        assertThat(service.configurado()).isTrue();
    }

    @Test
    void modeloDoSmtpTrocaCodigoEValidade() {
        EmailService service = new EmailService("", "", "smtp.gmail.com", 587, "Renovaí", "-");
        String html =
                service.modelo("codigo-redefinicao.html", java.util.Map.of("CODIGO", "123456", "VALIDADE", "15"));
        assertThat(html).contains("123456").contains("15 minutos").doesNotContain("{{");
    }
}
