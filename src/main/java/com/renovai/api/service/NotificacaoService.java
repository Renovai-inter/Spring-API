package com.renovai.api.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.firebase.cloud.FirestoreClient;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Notificações push (Firebase Cloud Messaging) dos eventos do app. Cada aviso é enviado depois do
 * commit da transação, fora da thread da requisição, para os aparelhos das contas interessadas,
 * nunca para quem fez a ação. Sem FIREBASE_CREDENTIALS configurado o push fica desligado e a API
 * segue funcionando normalmente.
 *
 * <p>Os aparelhos ficam no Firestore do mesmo projeto Firebase (coleção dispositivos_push, um
 * documento por token com o e-mail da conta), sem mudar o banco da API. As regras do Firestore
 * fecham a coleção para o app: só a API (SDK de admin) lê e grava.
 *
 * <p>Gestor: proposta recebida, proposta aceita/finalizada, coletas e triagens. Cooperado: estoque
 * abaixo de 1.000 kg e rateio em que entrou. Empresa: contraproposta e proposta finalizada.
 */
@Service
public class NotificacaoService {

    static final BigDecimal LIMITE_ESTOQUE_KG = new BigDecimal("1000");

    /** Aba do app que o toque na notificação abre. */
    static final String ABA_PEDIDOS = "pedidos",
            ABA_PEDIDOS_PENDENTES = "pedidos_pendentes", // proposta recebida, à espera do gestor
            ABA_COLETAS = "coletas",
            ABA_ESTOQUE = "estoque",
            ABA_RATEIOS = "rateios";

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);
    private static final int TOKENS_POR_ENVIO = 500; // limite do FCM por multicast
    private static final int EMAILS_POR_CONSULTA = 30; // limite do "in" no Firestore
    private static final String COLECAO_DISPOSITIVOS = "dispositivos_push";

    private final JdbcTemplate jdbc;
    private final FirebaseMessaging fcm; // null = push desligado
    private final Firestore firestore;
    private final ExecutorService envios =
            Executors.newSingleThreadExecutor(
                    r -> {
                        Thread t = new Thread(r, "notificacoes-push");
                        t.setDaemon(true);
                        return t;
                    });

    public NotificacaoService(JdbcTemplate jdbc, @Value("${FIREBASE_CREDENTIALS:}") String credenciais) {
        this.jdbc = jdbc;
        FirebaseApp app = iniciarFirebase(credenciais);
        this.fcm = app == null ? null : FirebaseMessaging.getInstance(app);
        this.firestore = app == null ? null : FirestoreClient.getFirestore(app);
        if (app != null) log.info("Notificações push ligadas (Firebase {}).", app.getOptions().getProjectId());
    }

    /** FIREBASE_CREDENTIALS: o JSON da conta de serviço do Firebase ou o caminho do arquivo. */
    private static FirebaseApp iniciarFirebase(String credenciais) {
        if (credenciais == null || credenciais.isBlank()) {
            log.warn("FIREBASE_CREDENTIALS não configurado: notificações push desligadas.");
            return null;
        }
        try (InputStream in =
                credenciais.trim().startsWith("{")
                        ? new ByteArrayInputStream(credenciais.getBytes(StandardCharsets.UTF_8))
                        : new FileInputStream(credenciais.trim())) {
            FirebaseOptions opcoes =
                    FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(in)).build();
            String nome = "renovai-push";
            return FirebaseApp.getApps().stream()
                    .filter(a -> a.getName().equals(nome))
                    .findFirst()
                    .orElseGet(() -> FirebaseApp.initializeApp(opcoes, nome));
        } catch (Exception | LinkageError e) {
            // LinkageError: falta de classe do SDK no build (a v0.1.2 caía aqui); a API segue sem push
            log.error("Não foi possível iniciar o Firebase: notificações push desligadas.", e);
            return null;
        }
    }

    /** true quando o Firebase iniciou com FIREBASE_CREDENTIALS e os pushes estão sendo enviados. */
    public boolean ativo() {
        return fcm != null;
    }

    // ───────────────────────────── aparelhos ─────────────────────────────

    /** Liga o aparelho à conta (o token é o id: se outra conta usava o aparelho, ela sai). */
    public void registrarDispositivo(String token, String email) {
        if (firestore == null) return;
        try {
            firestore.collection(COLECAO_DISPOSITIVOS)
                    .document(token)
                    .set(Map.of(
                            "email", email.toLowerCase(Locale.ROOT),
                            "dataAtualizacao", java.time.Instant.now().toString()))
                    .get();
        } catch (Exception e) {
            log.warn("Não foi possível registrar o aparelho para notificações.", e);
        }
    }

    /** Logout: só a própria conta descadastra o aparelho. */
    public void removerDispositivo(String token, String email) {
        if (firestore == null) return;
        try {
            var doc = firestore.collection(COLECAO_DISPOSITIVOS).document(token);
            var atual = doc.get().get();
            if (atual.exists() && email.equalsIgnoreCase(atual.getString("email"))) doc.delete().get();
        } catch (Exception e) {
            log.warn("Não foi possível descadastrar o aparelho.", e);
        }
    }

    private List<String> tokens(Set<String> emails) {
        List<String> r = new ArrayList<>();
        List<String> lista = new ArrayList<>(emails);
        for (int i = 0; i < lista.size(); i += EMAILS_POR_CONSULTA) {
            List<String> lote = lista.subList(i, Math.min(lista.size(), i + EMAILS_POR_CONSULTA));
            try {
                for (QueryDocumentSnapshot d :
                        firestore.collection(COLECAO_DISPOSITIVOS)
                                .whereIn("email", new ArrayList<Object>(lote))
                                .get()
                                .get()
                                .getDocuments()) r.add(d.getId());
            } catch (Exception e) {
                log.warn("Não foi possível consultar os aparelhos.", e);
            }
        }
        return r;
    }

    private void descartarDispositivo(String token) {
        try {
            firestore.collection(COLECAO_DISPOSITIVOS).document(token).delete().get();
        } catch (Exception e) {
            log.warn("Não foi possível descartar o aparelho.", e);
        }
    }

    // ───────────────────────────── eventos ─────────────────────────────

    public void propostaRecebida(UUID cooperativaId, String empresaNome) {
        publicar(() -> gestores(cooperativaId), "Nova proposta recebida",
                nome(empresaNome, "Uma empresa") + " enviou um pedido para a cooperativa.",
                ABA_PEDIDOS_PENDENTES);
    }

    public void contraproposta(UUID empresaId, String cooperativaNome, BigDecimal valor) {
        publicar(() -> empresa(empresaId), "Nova contraproposta",
                nome(cooperativaNome, "A cooperativa") + " enviou uma contraproposta de "
                        + dinheiro(valor) + ".", ABA_PEDIDOS);
    }

    public void propostaAceita(UUID cooperativaId, String empresaNome, BigDecimal valor) {
        publicar(() -> gestores(cooperativaId), "Proposta aceita",
                nome(empresaNome, "A empresa") + " aceitou a proposta de " + dinheiro(valor)
                        + ". Conclua o pedido.", ABA_PEDIDOS);
    }

    public void propostaFinalizada(
            UUID cooperativaId, UUID empresaId, String cooperativaNome, String empresaNome, BigDecimal valor) {
        publicar(() -> empresa(empresaId), "Proposta finalizada",
                nome(cooperativaNome, "A cooperativa") + " finalizou o pedido (" + dinheiro(valor) + ").",
                ABA_PEDIDOS);
        publicar(() -> gestores(cooperativaId), "Proposta finalizada",
                "O pedido de " + nome(empresaNome, "a empresa") + " foi finalizado ("
                        + dinheiro(valor) + ").", ABA_PEDIDOS);
    }

    public void coletaRegistrada(UUID cooperativaId, String cooperadoNome, BigDecimal kg) {
        publicar(() -> gestores(cooperativaId), "Nova coleta registrada",
                nome(cooperadoNome, "Um cooperado") + " registrou uma coleta de " + kg(kg) + ".",
                ABA_COLETAS);
    }

    public void coletaMudouDeEstado(UUID cooperativaId, String cooperadoNome, String antes, String depois) {
        publicar(() -> gestores(cooperativaId), "Coleta mudou de estado",
                "Coleta de " + nome(cooperadoNome, "um cooperado") + ": " + nome(antes, "sem status")
                        + " → " + nome(depois, "sem status") + ".", ABA_COLETAS);
    }

    public void triagemCriada(UUID cooperativaId, String material, BigDecimal kg, String status) {
        publicar(() -> gestores(cooperativaId), "Coleta enviada para triagem",
                nome(material, "Material") + " (" + kg(kg) + ") entrou em triagem: "
                        + nome(status, "sem status") + ".", ABA_COLETAS);
    }

    public void triagemMudouDeEstado(UUID cooperativaId, String material, String antes, String depois) {
        publicar(() -> gestores(cooperativaId), "Triagem mudou de estado",
                "Triagem de " + nome(material, "material") + ": " + nome(antes, "sem status") + " → "
                        + nome(depois, "sem status") + ".", ABA_COLETAS);
    }

    /** Um aviso por participante, com o valor que ele recebeu. */
    public void rateioRealizado(Map<UUID, BigDecimal> valorPorFuncionario, String tipo) {
        String rotulo = tipo == null ? "" : " " + tipo.toLowerCase(Locale.ROOT);
        valorPorFuncionario.forEach(
                (funcionarioId, valor) ->
                        publicar(() -> emailDoFuncionario(funcionarioId), "Rateio realizado",
                                "Você recebeu " + dinheiro(valor) + " no rateio" + rotulo + ".",
                                ABA_RATEIOS));
    }

    /** Retrato do estoque antes de uma operação, para comparar depois em estoqueAlterado. */
    public Map<UUID, BigDecimal> estoque(UUID cooperativaId) {
        if (fcm == null || cooperativaId == null) return Map.of();
        Map<UUID, BigDecimal> r = new HashMap<>();
        jdbc.query(
                "select estoque_id, quantidade_kg from estoques where cooperativa_id=?",
                rs -> {
                    r.put(rs.getObject(1, UUID.class), rs.getBigDecimal(2));
                },
                cooperativaId);
        return r;
    }

    /** Avisa os cooperados dos materiais que passaram de >= 1.000 kg para menos. */
    public void estoqueAlterado(UUID cooperativaId, Map<UUID, BigDecimal> antes) {
        if (fcm == null || cooperativaId == null || antes == null || antes.isEmpty()) return;
        String autor = autorAtual();
        depoisDoCommit(
                () -> {
                    Map<UUID, BigDecimal> depois = new HashMap<>();
                    Map<UUID, String> materiais = new HashMap<>();
                    jdbc.query(
                            """
                            select e.estoque_id, e.quantidade_kg, cm.nome_categoria from estoques e
                            join materiais m on m.material_id=e.material_id
                            left join categorias_materiais cm on cm.categoria_id=m.categoria_id
                            where e.cooperativa_id=?
                            """,
                            rs -> {
                                UUID id = rs.getObject(1, UUID.class);
                                depois.put(id, rs.getBigDecimal(2));
                                materiais.put(id, rs.getString(3));
                            },
                            cooperativaId);
                    List<UUID> baixos = caiuAbaixoDoLimite(antes, depois);
                    if (baixos.isEmpty()) return;
                    Set<String> destinos = semAutor(cooperados(cooperativaId), autor);
                    for (UUID id : baixos) {
                        String material = nome(materiais.get(id), "Um material");
                        enviar(destinos, "Estoque baixo: " + material,
                                material + " está com " + kg(depois.get(id)) + ", abaixo de 1.000 kg.",
                                ABA_ESTOQUE);
                    }
                });
    }

    // ───────────────────────────── regras ─────────────────────────────

    /** Itens que estavam >= limite e agora estão abaixo (avisa uma vez por queda). */
    static List<UUID> caiuAbaixoDoLimite(Map<UUID, BigDecimal> antes, Map<UUID, BigDecimal> depois) {
        List<UUID> r = new ArrayList<>();
        depois.forEach(
                (id, kg) -> {
                    BigDecimal anterior = antes.get(id);
                    if (anterior != null
                            && kg != null
                            && anterior.compareTo(LIMITE_ESTOQUE_KG) >= 0
                            && kg.compareTo(LIMITE_ESTOQUE_KG) < 0) r.add(id);
                });
        return r;
    }

    /** Destinatários sem quem fez a ação (e-mails comparados sem diferenciar maiúsculas). */
    static Set<String> semAutor(Collection<String> emails, String autor) {
        Set<String> r = new HashSet<>();
        for (String e : emails) {
            if (e == null || e.isBlank()) continue;
            String email = e.trim().toLowerCase(Locale.ROOT);
            if (!email.equals(autor)) r.add(email);
        }
        return r;
    }

    // ───────────────────────────── envio ─────────────────────────────

    private void publicar(Supplier<Collection<String>> destinatarios, String titulo, String texto, String aba) {
        if (fcm == null) return;
        String autor = autorAtual();
        depoisDoCommit(() -> enviar(semAutor(destinatarios.get(), autor), titulo, texto, aba));
    }

    /** Só envia se a transação confirmar; roda fora da thread da requisição. */
    private void depoisDoCommit(Runnable tarefa) {
        Runnable assincrona =
                () ->
                        envios.execute(
                                () -> {
                                    try {
                                        tarefa.run();
                                    } catch (RuntimeException e) {
                                        log.warn("Falha ao enviar notificação push.", e);
                                    }
                                });
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            assincrona.run();
                        }
                    });
        } else {
            assincrona.run();
        }
    }

    private void enviar(Set<String> emails, String titulo, String texto, String aba) {
        if (emails.isEmpty()) return;
        List<String> tokens = tokens(emails);
        Map<String, String> dados = new LinkedHashMap<>();
        dados.put("titulo", titulo);
        dados.put("texto", texto);
        dados.put("aba", aba);
        for (int i = 0; i < tokens.size(); i += TOKENS_POR_ENVIO) {
            List<String> lote = tokens.subList(i, Math.min(tokens.size(), i + TOKENS_POR_ENVIO));
            // só "data": o app monta a notificação (canal e aba certos) mesmo em segundo plano
            MulticastMessage mensagem =
                    MulticastMessage.builder()
                            .addAllTokens(lote)
                            .putAllData(dados)
                            .setAndroidConfig(
                                    AndroidConfig.builder()
                                            .setPriority(AndroidConfig.Priority.HIGH)
                                            .build())
                            .build();
            try {
                BatchResponse resposta = fcm.sendEachForMulticast(mensagem);
                for (int j = 0; j < resposta.getResponses().size(); j++) {
                    FirebaseMessagingException erro = resposta.getResponses().get(j).getException();
                    if (erro != null
                            && (erro.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                                    || erro.getMessagingErrorCode()
                                            == MessagingErrorCode.INVALID_ARGUMENT)) {
                        descartarDispositivo(lote.get(j)); // app desinstalado ou token trocado
                    }
                }
            } catch (FirebaseMessagingException e) {
                log.warn("Falha ao enviar notificação push.", e);
            }
        }
    }

    // ───────────────────────────── destinatários ─────────────────────────────

    /** Gestores e administradores da cooperativa (funcionários e o perfil institucional). */
    private List<String> gestores(UUID cooperativaId) {
        List<String> r =
                new ArrayList<>(
                        jdbc.queryForList(
                                """
                                select u.email from funcionarios f
                                join usuarios u on u.usuario_id=f.usuario_id
                                join cargos c on c.cargo_id=f.cargo_id
                                where f.cooperativa_id=? and f.status_funcionario='ATIVO'
                                and (upper(c.cargo) like '%GESTOR%' or upper(c.cargo) in ('ADMIN','ROLE_ADMIN'))
                                """,
                                String.class,
                                cooperativaId));
        r.addAll(
                jdbc.queryForList(
                        "select email from perfis where cooperativa_id=? and esta_ativo=true",
                        String.class,
                        cooperativaId));
        return r;
    }

    private List<String> cooperados(UUID cooperativaId) {
        return jdbc.queryForList(
                """
                select u.email from funcionarios f
                join usuarios u on u.usuario_id=f.usuario_id
                join cargos c on c.cargo_id=f.cargo_id
                where f.cooperativa_id=? and f.status_funcionario='ATIVO'
                and upper(c.cargo) like '%COOPERADO%'
                """,
                String.class,
                cooperativaId);
    }

    private List<String> empresa(UUID empresaId) {
        return jdbc.queryForList(
                "select email from perfis where empresa_id=? and esta_ativo=true", String.class, empresaId);
    }

    private List<String> emailDoFuncionario(UUID funcionarioId) {
        return jdbc.queryForList(
                """
                select u.email from funcionarios f join usuarios u on u.usuario_id=f.usuario_id
                where f.funcionario_id=?
                """,
                String.class,
                funcionarioId);
    }

    private static String autorAtual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null || auth.getName() == null ? "" : auth.getName().toLowerCase(Locale.ROOT);
    }

    // ───────────────────────────── textos ─────────────────────────────

    private static String nome(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor;
    }

    private static String dinheiro(BigDecimal v) {
        return NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"))
                .format(v == null ? BigDecimal.ZERO : v)
                .replace(' ', ' ');
    }

    private static String kg(BigDecimal v) {
        BigDecimal kg = v == null ? BigDecimal.ZERO : v.stripTrailingZeros();
        NumberFormat f = NumberFormat.getNumberInstance(Locale.of("pt", "BR"));
        f.setMaximumFractionDigits(1);
        return f.format(kg) + " kg";
    }
}
