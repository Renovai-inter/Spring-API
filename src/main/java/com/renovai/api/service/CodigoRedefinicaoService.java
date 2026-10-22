package com.renovai.api.service;

import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.Perfil;
import com.renovai.api.model.Usuario;
import com.renovai.api.repository.PerfilRepository;
import com.renovai.api.repository.UsuarioRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redefinição de senha em duas etapas: a pessoa informa o e-mail cadastrado (1º fator) e recebe
 * nele um código de 6 dígitos (2º fator), que precisa digitar no app para definir a nova senha.
 *
 * <p>O código não é guardado em texto: em token_redefinicao fica só o SHA-256 de
 * "e-mail|código". Ele vale {@value #VALIDADE_MINUTOS} minutos, serve uma vez só e é
 * invalidado depois de {@value #MAX_TENTATIVAS} tentativas erradas.
 */
@Service
public class CodigoRedefinicaoService {

    static final int VALIDADE_MINUTOS = 15;
    static final int MAX_TENTATIVAS = 5;

    private static final Logger LOGGER = LoggerFactory.getLogger(CodigoRedefinicaoService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /** Tentativas erradas por e-mail (zera ao pedir um código novo ou ao acertar). */
    private final Map<String, Integer> tentativas = new ConcurrentHashMap<>();

    public CodigoRedefinicaoService(
            PerfilRepository perfilRepository,
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Gera e envia o código. Se o e-mail não tiver conta, responde 404 para o app avisar a pessoa
     * (decisão de produto: priorizar a clareza para o usuário sobre esconder quais e-mails têm conta).
     */
    @Transactional
    public void solicitar(String emailInformado) {
        String email = normalizar(emailInformado);
        Optional<Conta> conta = buscarConta(email);
        if (conta.isEmpty()) {
            throw new RecursoNaoEncontradoException("Não existe conta com este e-mail.");
        }
        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        conta.get().gravarToken(hash(email, codigo), LocalDateTime.now().plusMinutes(VALIDADE_MINUTOS));
        tentativas.remove(email);

        if (!emailService.configurado()) {
            // Sem MAIL_SCRIPT_URL/MAIL_SCRIPT_SEGREDO nem MAIL_USERNAME/MAIL_PASSWORD no servidor
            // (ex.: testes, ambiente local): o código fica gravado, mas não há como enviá-lo. Não derruba a requisição para não revelar
            // nada a quem pediu; o aviso fica no log do servidor.
            LOGGER.error(
                    "Código de redefinição gerado, mas o envio de e-mail não está configurado"
                            + " (defina MAIL_SCRIPT_URL e MAIL_SCRIPT_SEGREDO; ver docs/email-apps-script.md).");
            return;
        }

        String html =
                emailService.modelo(
                        "codigo-redefinicao.html",
                        Map.of("CODIGO", codigo, "VALIDADE", String.valueOf(VALIDADE_MINUTOS)));
        String texto =
                "Seu código para redefinir a senha do Renovaí é "
                        + codigo
                        + ".\nEle vale por "
                        + VALIDADE_MINUTOS
                        + " minutos. Se não foi você que pediu, ignore este e-mail.";
        emailService.enviar(email, "Seu código para redefinir a senha do Renovaí", html, texto);
    }

    /** Confere o código sem gastá-lo (o app usa para liberar a tela de nova senha). */
    @Transactional(noRollbackFor = RegraDeNegocioException.class)
    public void verificar(String emailInformado, String codigo) {
        String email = normalizar(emailInformado);
        validar(email, codigo);
    }

    /** Confere o código e troca a senha. Depois disso o código deixa de valer. */
    @Transactional(noRollbackFor = RegraDeNegocioException.class)
    public void redefinir(String emailInformado, String codigo, String novaSenha) {
        if (novaSenha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RegraDeNegocioException("Senha deve ter até 72 bytes.");
        }
        String email = normalizar(emailInformado);
        Conta conta = validar(email, codigo);
        conta.trocarSenha(passwordEncoder.encode(novaSenha));
        tentativas.remove(email);
    }

    private Conta validar(String email, String codigo) {
        Conta conta =
                buscarConta(email)
                        .filter(c -> c.token() != null)
                        .orElseThrow(
                                () ->
                                        new RegraDeNegocioException(
                                                "Código inválido ou expirado. Solicite um novo código."));
        LocalDateTime expiracao = conta.expiracao();
        if (expiracao == null || !LocalDateTime.now().isBefore(expiracao)) {
            conta.limparToken();
            throw new RegraDeNegocioException("Código expirado. Solicite um novo código.");
        }
        boolean confere =
                codigo != null
                        && MessageDigest.isEqual(
                                conta.token().getBytes(StandardCharsets.UTF_8),
                                hash(email, codigo.trim()).getBytes(StandardCharsets.UTF_8));
        if (!confere) {
            int erros = tentativas.merge(email, 1, Integer::sum);
            if (erros >= MAX_TENTATIVAS) {
                conta.limparToken();
                tentativas.remove(email);
                throw new RegraDeNegocioException(
                        "Muitas tentativas erradas. Solicite um novo código.");
            }
            int restantes = MAX_TENTATIVAS - erros;
            throw new RegraDeNegocioException(
                    "Código incorreto. "
                            + (restantes == 1
                                    ? "Resta 1 tentativa."
                                    : "Restam " + restantes + " tentativas."));
        }
        return conta;
    }

    /** Mesma ordem do login: perfil com senha, depois funcionário (usuário), depois perfil. */
    private Optional<Conta> buscarConta(String email) {
        Optional<Perfil> perfilComSenha =
                perfilRepository.findByEmailIgnoreCase(email).filter(p -> p.getSenhaHash() != null);
        if (perfilComSenha.isPresent()) {
            return perfilComSenha.map(ContaPerfil::new);
        }
        Optional<Usuario> usuario = usuarioRepository.findByEmailIgnoreCase(email);
        if (usuario.isPresent()) {
            return usuario.map(ContaUsuario::new);
        }
        return perfilRepository.findByEmailIgnoreCase(email).map(ContaPerfil::new);
    }

    private static String normalizar(String email) {
        if (email == null || email.isBlank()) {
            throw new RegraDeNegocioException("Informe o e-mail.");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    static String hash(String email, String codigo) {
        try {
            byte[] digest =
                    MessageDigest.getInstance("SHA-256")
                            .digest((email + "|" + codigo).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // ── conta (Perfil ou Usuario: as duas tabelas têm token_redefinicao/token_expiracao) ──

    private interface Conta {
        String token();

        LocalDateTime expiracao();

        void gravarToken(String token, LocalDateTime expiracao);

        void trocarSenha(String senhaHash);

        default void limparToken() {
            gravarToken(null, null);
        }
    }

    private final class ContaPerfil implements Conta {
        private final Perfil perfil;

        ContaPerfil(Perfil perfil) {
            this.perfil = perfil;
        }

        public String token() {
            return perfil.getTokenRedefinicao();
        }

        public LocalDateTime expiracao() {
            return perfil.getTokenExpiracao();
        }

        public void gravarToken(String token, LocalDateTime expiracao) {
            perfil.setTokenRedefinicao(token);
            perfil.setTokenExpiracao(expiracao);
            perfilRepository.save(perfil);
        }

        public void trocarSenha(String senhaHash) {
            perfil.setSenhaHash(senhaHash);
            gravarToken(null, null);
        }
    }

    private final class ContaUsuario implements Conta {
        private final Usuario usuario;

        ContaUsuario(Usuario usuario) {
            this.usuario = usuario;
        }

        public String token() {
            return usuario.getTokenRedefinicao();
        }

        public LocalDateTime expiracao() {
            return usuario.getTokenExpiracao();
        }

        public void gravarToken(String token, LocalDateTime expiracao) {
            usuario.setTokenRedefinicao(token);
            usuario.setTokenExpiracao(expiracao);
            usuarioRepository.save(usuario);
        }

        public void trocarSenha(String senhaHash) {
            usuario.setSenhaHash(senhaHash);
            gravarToken(null, null);
        }
    }
}
