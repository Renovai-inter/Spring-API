package com.renovai.api.service;

import com.renovai.api.exception.RegraDeNegocioException;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 * Envio de e-mails transacionais (ex.: código de redefinição de senha) por SMTP.
 *
 * <p>Configuração por variáveis de ambiente (padrão: Gmail com "senha de app"):
 *
 * <pre>
 * MAIL_USERNAME   conta que envia (padrão renovai.suporte@gmail.com)
 * MAIL_PASSWORD   senha de app do Gmail (16 letras; os espaços são ignorados)
 * MAIL_HOST       opcional, padrão smtp.gmail.com
 * MAIL_PORT       opcional, padrão 587 (STARTTLS)
 * MAIL_FROM_NAME  opcional, padrão "Renovaí"
 * </pre>
 */
@Service
public class EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    private final String usuario;
    private final String senha;
    private final String host;
    private final int porta;
    private final String nomeRemetente;
    private volatile JavaMailSenderImpl sender;

    public EmailService(
            @Value("${mail.username:renovai.suporte@gmail.com}") String usuario,
            @Value("${mail.password:}") String senha,
            @Value("${mail.host:smtp.gmail.com}") String host,
            @Value("${mail.port:587}") int porta,
            @Value("${mail.from-name:Renovaí}") String nomeRemetente) {
        this.usuario = usuario == null ? "" : usuario.trim();
        this.senha = senha == null ? "" : senha.replace(" ", "");
        this.host = host;
        this.porta = porta;
        this.nomeRemetente = nomeRemetente;
    }

    public boolean configurado() {
        return !usuario.isBlank() && !senha.isBlank();
    }

    /** Envia um e-mail HTML com versão em texto puro (para clientes que não exibem HTML). */
    public void enviar(String para, String assunto, String html, String textoPuro) {
        if (!configurado()) {
            throw new RegraDeNegocioException(
                    "O envio de e-mail não está configurado no servidor. Avise o suporte do Renovaí.");
        }
        try {
            JavaMailSenderImpl mail = sender();
            MimeMessage mensagem = mail.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, "UTF-8");
            helper.setFrom(new InternetAddress(usuario, nomeRemetente, "UTF-8"));
            helper.setTo(para);
            helper.setSubject(assunto);
            helper.setText(textoPuro, html);
            mail.send(mensagem);
        } catch (Exception e) {
            LOGGER.error("Falha ao enviar e-mail para {}", para, e);
            throw new RegraDeNegocioException(
                    "Não foi possível enviar o e-mail agora. Tente novamente em alguns minutos.");
        }
    }

    /** Lê um modelo em src/main/resources/templates/email e troca cada {{CHAVE}} pelo valor. */
    public String modelo(String arquivo, Map<String, String> valores) {
        try (InputStream in = new ClassPathResource("templates/email/" + arquivo).getInputStream()) {
            String html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            for (Map.Entry<String, String> v : valores.entrySet()) {
                html = html.replace("{{" + v.getKey() + "}}", v.getValue());
            }
            return html;
        } catch (IOException e) {
            throw new IllegalStateException("Modelo de e-mail não encontrado: " + arquivo, e);
        }
    }

    private JavaMailSenderImpl sender() {
        JavaMailSenderImpl atual = sender;
        if (atual == null) {
            synchronized (this) {
                if (sender == null) {
                    JavaMailSenderImpl novo = new JavaMailSenderImpl();
                    novo.setHost(host);
                    novo.setPort(porta);
                    novo.setUsername(usuario);
                    novo.setPassword(senha);
                    novo.setDefaultEncoding("UTF-8");
                    Properties props = novo.getJavaMailProperties();
                    props.put("mail.transport.protocol", "smtp");
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.starttls.enable", "true");
                    props.put("mail.smtp.starttls.required", "true");
                    props.put("mail.smtp.connectiontimeout", "10000");
                    props.put("mail.smtp.timeout", "10000");
                    props.put("mail.smtp.writetimeout", "10000");
                    sender = novo;
                }
                atual = sender;
            }
        }
        return atual;
    }
}
