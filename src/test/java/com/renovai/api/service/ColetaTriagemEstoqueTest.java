package com.renovai.api.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.renovai.api.dto.request.Requests.*;
import com.renovai.api.model.*;
import com.renovai.api.repository.*;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.math.BigDecimal;
import java.util.*;

class ColetaTriagemEstoqueTest {
    private JdbcTemplate jdbc;
    private DataSourceTransactionManager transactions;
    private ColetaService coletas;
    private TriagemService triagens;
    private MaterialRepository materiais;
    private final Map<UUID, Coleta> coletasSalvas = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<UUID, Triagem> triagensSalvas =
            new java.util.concurrent.ConcurrentHashMap<>();
    private Funcionario cooperado;
    private Equipe equipe;
    private Material papel, plastico;
    private Status pendente, concluido;

    @BeforeEach
    void preparar() {
        var ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(ds);
        transactions = new DataSourceTransactionManager(ds);
        jdbc.execute("create table cooperativas(cooperativa_id uuid primary key)");
        jdbc.execute(
                "create table coletas(evento_id uuid primary key,quantidade_kg numeric(10,3) not"
                        + " null)");
        jdbc.execute(
                "create table triagens(evento_id uuid primary key,coleta_id uuid not null"
                    + " references coletas,material_id uuid not null,status_id uuid,quantidade_kg"
                    + " numeric(10,3),quantidade_rejeito_kg numeric(10,3))");
        jdbc.execute(
                "create table estoques(estoque_id uuid primary key,cooperativa_id uuid not null"
                    + " references cooperativas,material_id uuid not null,quantidade_kg"
                    + " numeric(10,3) not null check(quantidade_kg>=0),data_atualizacao timestamp"
                    + " default current_timestamp,unique(cooperativa_id,material_id))");
        jdbc.execute(
                "create table movimentacoes_estoques(movimentacao_id uuid default random_uuid()"
                    + " primary key,estoque_id uuid not null references estoques,triagem_id uuid"
                    + " references triagens,item_id uuid,quantidade_kg numeric(10,3) not null"
                    + " check(quantidade_kg<>0),tipo_movimentacao varchar(10),check((triagem_id is"
                    + " not null and item_id is null) or (triagem_id is null and item_id is not"
                    + " null)))");
        var cooperativa = new Cooperativa();
        cooperativa.setCooperativaId(UUID.randomUUID());
        jdbc.update("insert into cooperativas values(?)", cooperativa.getCooperativaId());
        cooperado = new Funcionario();
        cooperado.setFuncionarioId(UUID.randomUUID());
        cooperado.setCooperativa(cooperativa);
        equipe = new Equipe();
        equipe.setEquipeId(UUID.randomUUID());
        equipe.setGestor(cooperado);
        papel = material();
        plastico = material();
        pendente = status("Em triagem");
        concluido = status("Concluído");
        var coletaRepository = mock(ColetaRepository.class);
        var triagemRepository = mock(TriagemRepository.class);
        var funcionarios = mock(FuncionarioRepository.class);
        var equipes = mock(EquipeRepository.class);
        var statuses = mock(StatusRepository.class);
        materiais = mock(MaterialRepository.class);
        when(funcionarios.findById(cooperado.getFuncionarioId()))
                .thenReturn(Optional.of(cooperado));
        when(equipes.findById(equipe.getEquipeId())).thenReturn(Optional.of(equipe));
        when(materiais.findById(papel.getMaterialId())).thenReturn(Optional.of(papel));
        when(materiais.findById(plastico.getMaterialId())).thenReturn(Optional.of(plastico));
        for (Status s : List.of(pendente, concluido)) {
            when(statuses.findById(s.getStatusId())).thenReturn(Optional.of(s));
            when(statuses.findByReferenciaAndStatusAtual("TRIAGEM", s.getStatusAtual()))
                    .thenReturn(Optional.of(s));
        }
        when(coletaRepository.saveAndFlush(any()))
                .thenAnswer(
                        inv -> {
                            Coleta c = inv.getArgument(0);
                            if (c.getEventoId() == null) c.setEventoId(UUID.randomUUID());
                            jdbc.update(
                                    "merge into coletas key(evento_id) values(?,?)",
                                    c.getEventoId(),
                                    c.getQuantidadeKg());
                            coletasSalvas.put(c.getEventoId(), c);
                            return c;
                        });
        when(coletaRepository.findById(any()))
                .thenAnswer(inv -> Optional.ofNullable(coletasSalvas.get(inv.getArgument(0))));
        when(triagemRepository.saveAndFlush(any()))
                .thenAnswer(
                        inv -> {
                            Triagem t = inv.getArgument(0);
                            if (t.getEventoId() == null) t.setEventoId(UUID.randomUUID());
                            jdbc.update(
                                    "merge into triagens key(evento_id) values(?,?,?,?,?,?)",
                                    t.getEventoId(),
                                    t.getColeta().getEventoId(),
                                    t.getMaterial().getMaterialId(),
                                    t.getStatus().getStatusId(),
                                    t.getQuantidadeKg(),
                                    t.getQuantidadeRejeitoKg());
                            triagensSalvas.put(t.getEventoId(), t);
                            return t;
                        });
        when(triagemRepository.buscarComBloqueio(any()))
                .thenAnswer(inv -> Optional.ofNullable(triagensSalvas.get(inv.getArgument(0))));
        when(triagemRepository.findByColeta_EventoId(any()))
                .thenAnswer(
                        inv ->
                                triagensSalvas.values().stream()
                                        .filter(
                                                t ->
                                                        t.getColeta()
                                                                .getEventoId()
                                                                .equals(inv.getArgument(0)))
                                        .toList());
        var estoque = proxy(new TriagemEstoqueService(jdbc));
        triagens =
                proxy(
                        new TriagemService(
                                triagemRepository,
                                equipes,
                                coletaRepository,
                                materiais,
                                statuses,
                                mock(EquipeCooperadoRepository.class),
                                estoque));
        coletas =
                proxy(
                        new ColetaService(
                                coletaRepository,
                                funcionarios,
                                statuses,
                                mock(RotaRepository.class),
                                triagemRepository,
                                triagens,
                                estoque));
    }

    @Test
    void materialSeparadoEntraNoEstoqueNaColeta() {
        var response = coletas.criar(request(false, List.of(item(papel, "100"))));
        assertThat(response.precisaTriagem()).isFalse();
        assertThat(response.materiais()).hasSize(1);
        assertThat(saldo(papel)).isEqualByComparingTo("100");
        assertThat(movimentos()).isEqualTo(1);
    }

    @Test
    void variosMateriaisSeparadosEntramEmSeusEstoques() {
        var response =
                coletas.criar(request(false, List.of(item(papel, "60"), item(plastico, "40"))));
        assertThat(response.materiais()).hasSize(2);
        assertThat(saldo(papel)).isEqualByComparingTo("60");
        assertThat(saldo(plastico)).isEqualByComparingTo("40");
    }

    @Test
    void materialMisturadoSoEntraAoConcluirSemSubtrairRejeitoDoPesoAproveitavel() {
        var response = coletas.criar(request(true, List.of(item(papel, "100"))));
        assertThat(response.precisaTriagem()).isTrue();
        assertThat(movimentos()).isZero();
        UUID id = response.materiais().get(0).triagemId();
        triagens.atualizar(
                id,
                new TriagemRequest(
                        equipe.getEquipeId(),
                        response.coletaId(),
                        papel.getMaterialId(),
                        pendente.getStatusId(),
                        new BigDecimal("40"),
                        new BigDecimal("60"),
                        null));
        assertThat(movimentos()).isZero();
        triagens.concluir(id, new ConcluirTriagemRequest(new BigDecimal("40"), null));
        assertThat(saldo(papel)).isEqualByComparingTo("40");
        assertThat(coletas.buscarPorId(response.coletaId()).precisaTriagem()).isFalse();
    }

    @Test
    void conclusaoRepetidaNaoDuplicaEntradaECorrecaoAplicaSomenteDiferenca() {
        var response = coletas.criar(request(true, List.of(item(papel, "100"))));
        UUID id = response.materiais().get(0).triagemId();
        triagens.concluir(id, new ConcluirTriagemRequest(new BigDecimal("100"), null));
        triagens.concluir(id, new ConcluirTriagemRequest(new BigDecimal("100"), null));
        assertThat(movimentos()).isEqualTo(1);
        triagens.concluir(id, new ConcluirTriagemRequest(new BigDecimal("80"), null));
        assertThat(saldo(papel)).isEqualByComparingTo("80");
        assertThat(movimentos()).isEqualTo(2);
    }

    @Test
    void reabrirTriagemEstornaEntradaEStatusConcluidoRegistraNovamente() {
        UUID id =
                coletas.criar(request(false, List.of(item(papel, "100"))))
                        .materiais()
                        .get(0)
                        .triagemId();
        triagens.atualizarStatus(id, new AtualizarStatusTriagemRequest(pendente.getStatusId()));
        assertThat(saldo(papel)).isEqualByComparingTo("0");
        triagens.atualizarStatus(id, new AtualizarStatusTriagemRequest(concluido.getStatusId()));
        assertThat(saldo(papel)).isEqualByComparingTo("100");
    }

    @Test
    void triggerDeMovimentacaoNaoDuplicaEntrada() {
        jdbc.execute(
                "create trigger saldo after insert on movimentacoes_estoques for each row call"
                        + " 'com.renovai.api.service.ColetaTriagemEstoqueTest$SaldoTrigger'");
        UUID id =
                coletas.criar(request(false, List.of(item(papel, "100"))))
                        .materiais()
                        .get(0)
                        .triagemId();
        assertThat(saldo(papel)).isEqualByComparingTo("100");
        triagens.concluir(id, new ConcluirTriagemRequest(new BigDecimal("80"), null));
        assertThat(saldo(papel)).isEqualByComparingTo("80");
    }

    @Test
    void falhaNoSegundoMaterialReverteColetaTriagensMovimentosEEstoque() {
        when(materiais.findById(plastico.getMaterialId())).thenReturn(Optional.empty());
        assertThatThrownBy(
                        () ->
                                coletas.criar(
                                        request(
                                                false,
                                                List.of(item(papel, "60"), item(plastico, "40")))))
                .isInstanceOf(com.renovai.api.exception.RecursoNaoEncontradoException.class);
        for (String tabela : List.of("coletas", "triagens", "movimentacoes_estoques", "estoques"))
            assertThat(jdbc.queryForObject("select count(*) from " + tabela, Integer.class))
                    .isZero();
    }

    @Test
    void correcaoQueExcedeEstoqueDisponivelReverteTriagem() {
        UUID id =
                coletas.criar(request(false, List.of(item(papel, "100"))))
                        .materiais()
                        .get(0)
                        .triagemId();
        jdbc.update("update estoques set quantidade_kg=50");
        assertThatThrownBy(
                        () ->
                                triagens.concluir(
                                        id, new ConcluirTriagemRequest(new BigDecimal("10"), null)))
                .hasMessageContaining("Estoque insuficiente");
        assertThat(
                        jdbc.queryForObject(
                                "select quantidade_kg from triagens where evento_id=?",
                                BigDecimal.class,
                                id))
                .isEqualByComparingTo("100");
        assertThat(saldo(papel)).isEqualByComparingTo("50");
        assertThat(movimentos()).isEqualTo(1);
    }

    @Test
    void editarColetaSeparadaCorrigeEstoqueSemDuplicar() {
        var response = coletas.criar(request(false, List.of(item(papel, "100"))));
        var request =
                new ColetaRequest(
                        cooperado.getFuncionarioId(),
                        null,
                        new BigDecimal("80"),
                        null,
                        "INTERNA",
                        null,
                        equipe.getEquipeId(),
                        false,
                        List.of(item(papel, "80")));
        coletas.atualizar(response.coletaId(), request);
        coletas.atualizar(response.coletaId(), request);
        assertThat(saldo(papel)).isEqualByComparingTo("80");
        assertThat(movimentos()).isEqualTo(2);
    }

    @Test
    void coletaComEntradaNaoPodeAlterarPesoSemInformarMateriais() {
        var response = coletas.criar(request(false, List.of(item(papel, "100"))));
        var request =
                new ColetaRequest(
                        cooperado.getFuncionarioId(),
                        null,
                        new BigDecimal("80"),
                        null,
                        "INTERNA",
                        null,
                        null,
                        null,
                        null);
        assertThatThrownBy(() -> coletas.atualizar(response.coletaId(), request))
                .hasMessageContaining("Informe os pesos dos materiais");
        assertThat(saldo(papel)).isEqualByComparingTo("100");
        assertThat(jdbc.queryForObject("select quantidade_kg from coletas", BigDecimal.class))
                .isEqualByComparingTo("100");
    }

    @Test
    void entradaImediataSemMaterialEPesosDivergentesSaoRejeitados() {
        assertThatThrownBy(() -> coletas.criar(request(false, null)))
                .hasMessageContaining("Informe os materiais");
        assertThatThrownBy(() -> coletas.criar(request(false, List.of(item(papel, "80")))))
                .hasMessageContaining("soma dos pesos");
        assertThat(jdbc.queryForObject("select count(*) from coletas", Integer.class)).isZero();
    }

    @Test
    void conclusoesConcorrentesNaoDuplicamEntrada() throws Exception {
        UUID id =
                coletas.criar(request(true, List.of(item(papel, "100"))))
                        .materiais()
                        .get(0)
                        .triagemId();
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var primeira =
                    executor.submit(
                            () ->
                                    triagens.concluir(
                                            id,
                                            new ConcluirTriagemRequest(
                                                    new BigDecimal("100"), null)));
            var segunda =
                    executor.submit(
                            () ->
                                    triagens.concluir(
                                            id,
                                            new ConcluirTriagemRequest(
                                                    new BigDecimal("100"), null)));
            primeira.get(10, java.util.concurrent.TimeUnit.SECONDS);
            segunda.get(10, java.util.concurrent.TimeUnit.SECONDS);
        }
        assertThat(saldo(papel)).isEqualByComparingTo("100");
        assertThat(movimentos()).isEqualTo(1);
    }

    private ColetaRequest request(boolean precisa, List<ColetaMaterialRequest> materiais) {
        return new ColetaRequest(
                cooperado.getFuncionarioId(),
                null,
                new BigDecimal("100"),
                null,
                "INTERNA",
                null,
                equipe.getEquipeId(),
                precisa,
                materiais);
    }

    private ColetaMaterialRequest item(Material material, String peso) {
        return new ColetaMaterialRequest(material.getMaterialId(), new BigDecimal(peso));
    }

    private Material material() {
        var material = new Material();
        material.setMaterialId(UUID.randomUUID());
        return material;
    }

    private Status status(String nome) {
        var status = new Status();
        status.setStatusId(UUID.randomUUID());
        status.setReferencia("TRIAGEM");
        status.setStatusAtual(nome);
        return status;
    }

    private BigDecimal saldo(Material material) {
        return jdbc.queryForObject(
                "select quantidade_kg from estoques where material_id=?",
                BigDecimal.class,
                material.getMaterialId());
    }

    private int movimentos() {
        return jdbc.queryForObject("select count(*) from movimentacoes_estoques", Integer.class);
    }

    public static class SaldoTrigger implements org.h2.api.Trigger {
        public void fire(java.sql.Connection connection, Object[] oldRow, Object[] newRow)
                throws java.sql.SQLException {
            try (var statement =
                    connection.prepareStatement(
                            "update estoques set quantidade_kg=quantidade_kg+? where"
                                    + " estoque_id=?")) {
                statement.setObject(1, newRow[4]);
                statement.setObject(2, newRow[1]);
                statement.executeUpdate();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(T target) {
        var factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(
                new TransactionInterceptor(
                        transactions, new AnnotationTransactionAttributeSource()));
        return (T) factory.getProxy();
    }
}
