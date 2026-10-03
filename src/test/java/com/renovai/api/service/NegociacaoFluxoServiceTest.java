package com.renovai.api.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.renovai.api.model.*;
import com.renovai.api.repository.*;

import org.h2.api.Trigger;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class NegociacaoFluxoServiceTest {
    private JdbcTemplate jdbc;
    private NegociacaoFluxoService service;
    private NegociacaoRepository negociacoes;
    private NegociacaoMensagemRepository mensagens;
    private PerfilRepository perfis;
    private Negociacao n;
    private UUID estoqueId, materialId, itemId;
    private Status aceito, finalizado;
    private static final String EMPRESA = "empresa@teste.com", GESTOR = "gestor@teste.com";

    @BeforeEach
    void preparar() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("create table empresas(empresa_id uuid primary key,nome varchar(255))");
        jdbc.execute(
                "create table pedidos(pedido_id uuid primary key,empresa_id uuid,data_pedido"
                        + " timestamp default current_timestamp,data_conclusao timestamp,observacao"
                        + " text)");
        jdbc.execute(
                "create table negociacoes(negociacao_id uuid primary key,pedido_id"
                        + " uuid,cooperativa_id uuid,status_id uuid,valor_total"
                        + " decimal(10,2),data_fechamento timestamp)");
        jdbc.execute("create table status(status_id uuid primary key,status_atual varchar(50))");
        jdbc.execute(
                "create table pedidos_cooperativas(pedido_cooperativa_id uuid default"
                        + " random_uuid(),pedido_id uuid,cooperativa_id uuid,status_id uuid)");
        jdbc.execute(
                "create table pedido_itens(item_id uuid primary key,pedido_id uuid,material_id"
                        + " uuid,quantidade_kg decimal(10,3),preco_unitario decimal(10,2))");
        jdbc.execute(
                "create table negociacao_itens(negociacao_id uuid,material_id uuid,quantidade_kg"
                        + " decimal(10,3),preco_unitario decimal(10,2))");
        jdbc.execute(
                "create table estoques(estoque_id uuid primary key,cooperativa_id uuid,material_id"
                        + " uuid,quantidade_kg decimal(10,3),data_atualizacao timestamp)");
        jdbc.execute(
                "create table movimentacoes_estoques(movimentacao_id uuid default"
                        + " random_uuid(),estoque_id uuid,item_id uuid,quantidade_kg"
                        + " decimal(10,3),tipo_movimentacao varchar(10))");
        negociacoes = mock(NegociacaoRepository.class);
        StatusRepository statuses = mock(StatusRepository.class);
        perfis = mock(PerfilRepository.class);
        mensagens = mock(NegociacaoMensagemRepository.class);
        var target =
                new NegociacaoFluxoService(
                        negociacoes,
                        statuses,
                        perfis,
                        mock(UsuarioRepository.class),
                        mock(FuncionarioRepository.class),
                        mensagens,
                        jdbc);
        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(
                new TransactionInterceptor(
                        new DataSourceTransactionManager(ds),
                        new AnnotationTransactionAttributeSource()));
        service = (NegociacaoFluxoService) factory.getProxy();
        Empresa empresa = new Empresa();
        empresa.setEmpresaId(UUID.randomUUID());
        Cooperativa coop = new Cooperativa();
        coop.setCooperativaId(UUID.randomUUID());
        Pedido pedido = new Pedido();
        pedido.setPedidoId(UUID.randomUUID());
        pedido.setEmpresa(empresa);
        n = new Negociacao();
        n.setNegociacaoId(UUID.randomUUID());
        n.setPedido(pedido);
        n.setEmpresa(empresa);
        n.setCooperativa(coop);
        n.setStatus(status("Em andamento"));
        n.setValorTotal(new BigDecimal("12.00"));
        aceito = status("Aceito");
        finalizado = status("Finalizado");
        when(statuses.findByReferencia("NEGOCIACAO"))
                .thenReturn(
                        List.of(
                                n.getStatus(),
                                status("Acordo fechado"),
                                status("Negociação recusada")));
        when(statuses.findByReferencia("PEDIDO"))
                .thenReturn(List.of(aceito, finalizado, status("Recusado")));
        Perfil empresaPerfil = new Perfil();
        empresaPerfil.setPerfilId(UUID.randomUUID());
        empresaPerfil.setEmpresa(empresa);
        empresaPerfil.setEstaAtivo(true);
        Perfil gestorPerfil = new Perfil();
        gestorPerfil.setPerfilId(UUID.randomUUID());
        gestorPerfil.setCooperativa(coop);
        gestorPerfil.setEstaAtivo(true);
        when(perfis.findByEmailIgnoreCase(EMPRESA)).thenReturn(Optional.of(empresaPerfil));
        when(perfis.findByEmailIgnoreCase(GESTOR)).thenReturn(Optional.of(gestorPerfil));
        when(negociacoes.buscarComBloqueio(n.getNegociacaoId())).thenReturn(Optional.of(n));
        when(negociacoes.saveAndFlush(any()))
                .thenAnswer(
                        inv -> {
                            Negociacao salvo = inv.getArgument(0);
                            jdbc.update(
                                    "update negociacoes set"
                                            + " status_id=?,valor_total=?,data_fechamento=? where"
                                            + " negociacao_id=?",
                                    salvo.getStatus().getStatusId(),
                                    salvo.getValorTotal(),
                                    salvo.getDataFechamento(),
                                    salvo.getNegociacaoId());
                            return salvo;
                        });
        estoqueId = UUID.randomUUID();
        materialId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        jdbc.update("insert into empresas values(?,'Empresa')", empresa.getEmpresaId());
        jdbc.update(
                "insert into pedidos(pedido_id,empresa_id) values(?,?)",
                pedido.getPedidoId(),
                empresa.getEmpresaId());
        jdbc.update(
                "insert into negociacoes(negociacao_id,pedido_id,cooperativa_id) values(?,?,?)",
                n.getNegociacaoId(),
                pedido.getPedidoId(),
                coop.getCooperativaId());
        for (Status s : List.of(n.getStatus(), aceito, finalizado))
            jdbc.update("insert into status values(?,?)", s.getStatusId(), s.getStatusAtual());
        jdbc.update(
                "insert into pedidos_cooperativas(pedido_id,cooperativa_id,status_id)"
                        + " values(?,?,?)",
                pedido.getPedidoId(),
                coop.getCooperativaId(),
                n.getStatus().getStatusId());
        jdbc.update(
                "insert into pedido_itens values(?,?,?,6,5)",
                itemId,
                pedido.getPedidoId(),
                materialId);
        jdbc.update(
                "insert into negociacao_itens values(?,?,6,2)", n.getNegociacaoId(), materialId);
        jdbc.update(
                "insert into estoques(estoque_id,cooperativa_id,material_id,quantidade_kg)"
                        + " values(?,?,?,10)",
                estoqueId,
                coop.getCooperativaId(),
                materialId);
    }

    @Test
    void aceiteSemTriggerAtualizaSaldoEVinculoSemConcluirERepeticaoNaoDuplicaSaida() {
        service.aceitar(n.getNegociacaoId(), EMPRESA);
        service.aceitar(n.getNegociacaoId(), EMPRESA);
        assertThat(saldo()).isEqualByComparingTo("4");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from movimentacoes_estoques", Integer.class))
                .isEqualTo(1);
        assertThat(n.getStatus().getStatusAtual()).isEqualTo("Acordo fechado");
        assertThat(n.getDataFechamento()).isNull();
        assertThat(jdbc.queryForObject("select status_id from pedidos_cooperativas", UUID.class))
                .isEqualTo(aceito.getStatusId());
        assertThat(
                        jdbc.queryForObject(
                                "select data_conclusao from pedidos", java.sql.Timestamp.class))
                .isNull();
    }

    @Test
    void aceiteComTriggerNaoDescontaSaldoDuasVezes() {
        jdbc.execute(
                "create trigger saldo after insert on movimentacoes_estoques for each row call '"
                        + SaldoTrigger.class.getName()
                        + "'");
        service.aceitar(n.getNegociacaoId(), EMPRESA);
        assertThat(saldo()).isEqualByComparingTo("4");
    }

    @Test
    void falhaDepoisDeRegistrarMovimentacaoReverteTodaTransacao() {
        doThrow(new IllegalStateException("Falha ao salvar negociação"))
                .when(negociacoes)
                .saveAndFlush(any());
        assertThatThrownBy(() -> service.aceitar(n.getNegociacaoId(), EMPRESA))
                .hasMessage("Falha ao salvar negociação");
        assertThat(saldo()).isEqualByComparingTo("10");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from movimentacoes_estoques", Integer.class))
                .isZero();
        verify(negociacoes).saveAndFlush(any());
    }

    @Test
    void estoqueInsuficienteNaoGravaSaidaNemMudaStatus() {
        jdbc.update("update estoques set quantidade_kg=5");
        assertThatThrownBy(() -> service.aceitar(n.getNegociacaoId(), EMPRESA))
                .hasMessageContaining("Estoque insuficiente");
        assertThat(saldo()).isEqualByComparingTo("5");
        verify(negociacoes, never()).saveAndFlush(any());
    }

    @Test
    void gestorConcluiDepoisDoAceiteSemNovaBaixaERepeticaoMantemData() {
        service.aceitar(n.getNegociacaoId(), EMPRESA);
        service.concluir(n.getNegociacaoId(), GESTOR, new BigDecimal("12"), null);
        var data = n.getDataFechamento();
        service.concluir(n.getNegociacaoId(), GESTOR, new BigDecimal("12"), null);
        assertThat(n.getDataFechamento()).isEqualTo(data);
        assertThat(jdbc.queryForObject("select status_id from pedidos_cooperativas", UUID.class))
                .isEqualTo(finalizado.getStatusId());
        assertThat(
                        jdbc.queryForObject(
                                "select data_conclusao from pedidos", java.sql.Timestamp.class))
                .isNotNull();
        assertThat(saldo()).isEqualByComparingTo("4");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from movimentacoes_estoques", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void conclusaoPreservaDataOriginalDoPedido() {
        service.aceitar(n.getNegociacaoId(), EMPRESA);
        var data = java.sql.Timestamp.valueOf("2026-09-01 10:00:00");
        jdbc.update("update pedidos set data_conclusao=?", data);

        service.concluir(n.getNegociacaoId(), GESTOR, new BigDecimal("12"), null);

        assertThat(
                        jdbc.queryForObject(
                                "select data_conclusao from pedidos", java.sql.Timestamp.class))
                .isEqualTo(data);
    }

    @Test
    void recusaDoUltimoVinculoEncerraPedidoSemAlterarDataNaRepeticao() {
        Status recusado = status("Recusado");
        jdbc.update(
                "insert into status values(?,?)",
                recusado.getStatusId(),
                recusado.getStatusAtual());
        jdbc.update(
                "insert into pedidos_cooperativas(pedido_id,cooperativa_id,status_id)"
                    + " values(?,?,?)",
                n.getPedido().getPedidoId(),
                UUID.randomUUID(),
                finalizado.getStatusId());

        service.recusar(n.getNegociacaoId(), EMPRESA, "Recusado.");

        var data =
                jdbc.queryForObject("select data_conclusao from pedidos", java.sql.Timestamp.class);
        assertThat(data).isNotNull();
        service.recusar(n.getNegociacaoId(), EMPRESA, "Recusado.");
        assertThat(
                        jdbc.queryForObject(
                                "select data_conclusao from pedidos", java.sql.Timestamp.class))
                .isEqualTo(data);
        assertThat(saldo()).isEqualByComparingTo("10");
    }

    @Test
    void recusaMantemPedidoAbertoEnquantoOutroVinculoNaoFoiEncerrado() {
        Status recusado = status("Recusado");
        jdbc.update(
                "insert into status values(?,?)",
                recusado.getStatusId(),
                recusado.getStatusAtual());
        jdbc.update(
                "insert into pedidos_cooperativas(pedido_id,cooperativa_id,status_id)"
                    + " values(?,?,?)",
                n.getPedido().getPedidoId(),
                UUID.randomUUID(),
                aceito.getStatusId());

        service.recusar(n.getNegociacaoId(), EMPRESA, "Recusado.");

        assertThat(
                        jdbc.queryForObject(
                                "select data_conclusao from pedidos", java.sql.Timestamp.class))
                .isNull();
    }

    @Test
    void empresaNaoPodeConcluirEGestorNaoPodeAceitar() {
        assertThatThrownBy(
                        () ->
                                service.concluir(
                                        n.getNegociacaoId(), EMPRESA, new BigDecimal("12"), null))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.aceitar(n.getNegociacaoId(), GESTOR))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void conclusaoSemAceiteEAlteracaoDoValorAceitoSaoRejeitadas() {
        assertThatThrownBy(
                        () ->
                                service.concluir(
                                        n.getNegociacaoId(), GESTOR, new BigDecimal("12"), null))
                .hasMessageContaining("precisa aceitar");
        service.aceitar(n.getNegociacaoId(), EMPRESA);
        assertThatThrownBy(
                        () ->
                                service.concluir(
                                        n.getNegociacaoId(), GESTOR, new BigDecimal("13"), null))
                .hasMessageContaining("valor aceito");
        assertThat(n.getDataFechamento()).isNull();
    }

    @Test
    void observacaoDaContrapropostaFicaNaConversaComRemetenteReal() {
        service.registrarObservacao(n, GESTOR, "Retirar na sexta-feira", "CONTRAPROPOSTA");
        var captor = org.mockito.ArgumentCaptor.forClass(NegociacaoMensagem.class);
        verify(mensagens).save(captor.capture());
        assertThat(captor.getValue().getMensagem()).isEqualTo("Retirar na sexta-feira");
        assertThat(captor.getValue().getTipoMensagem()).isEqualTo("CONTRAPROPOSTA");
        assertThat(captor.getValue().getRemetente().getCooperativa()).isSameAs(n.getCooperativa());
    }

    @Test
    void contaDeOutraEmpresaNaoAcessaNegociacao() {
        Perfil outro = new Perfil();
        outro.setEstaAtivo(true);
        Empresa e = new Empresa();
        e.setEmpresaId(UUID.randomUUID());
        outro.setEmpresa(e);
        when(perfis.findByEmailIgnoreCase("outra@teste.com")).thenReturn(Optional.of(outro));
        assertThatThrownBy(() -> service.aceitar(n.getNegociacaoId(), "outra@teste.com"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void pedidosEDashboardUsamValorNegociadoEmVezDoPrecoOriginal() throws Exception {
        service.aceitar(n.getNegociacaoId(), EMPRESA);
        var repo = new EmpresaSchemaRepository(jdbc);
        var pedido = repo.listarPedidosPorEmpresa(n.getEmpresa().getEmpresaId()).get(0);
        assertThat(pedido.statusAtual()).isEqualTo("Aceito");
        assertThat(pedido.valorTotal()).isEqualByComparingTo("12");
        var query =
                NegociacaoRepository.class
                        .getMethod("sumValorNegociadoByEmpresa", UUID.class)
                        .getAnnotation(org.springframework.data.jpa.repository.Query.class)
                        .value();
        assertThat(
                        jdbc.queryForObject(
                                query.replace(":empresaId", "?"),
                                BigDecimal.class,
                                n.getEmpresa().getEmpresaId()))
                .isEqualByComparingTo("12");
        service.concluir(n.getNegociacaoId(), GESTOR, new BigDecimal("12"), null);
        assertThat(repo.contarPedidosConcluidos(n.getEmpresa().getEmpresaId())).isEqualTo(1L);
        assertThat(repo.listarPedidosPorEmpresa(n.getEmpresa().getEmpresaId()).get(0).statusAtual())
                .isEqualTo("Finalizado");
    }

    @Test
    void saidaPreexistenteDoMesmoPedidoImpedeBaixaDuplicada() {
        jdbc.update(
                "insert into movimentacoes_estoques(estoque_id,item_id,quantidade_kg)"
                        + " values(?,?,-6)",
                estoqueId,
                itemId);
        assertThatThrownBy(() -> service.aceitar(n.getNegociacaoId(), EMPRESA))
                .hasMessageContaining("já possui saída");
        assertThat(saldo()).isEqualByComparingTo("10");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from movimentacoes_estoques", Integer.class))
                .isEqualTo(1);
    }

    private Status status(String nome) {
        Status s = new Status();
        s.setStatusId(UUID.randomUUID());
        s.setStatusAtual(nome);
        return s;
    }

    @Test
    void contrapropostaSomenteDeValorMantemPesosOriginaisESalvaSnapshotNoAceite() {
        jdbc.update("delete from negociacao_itens");
        service.aceitar(n.getNegociacaoId(), EMPRESA);
        assertThat(saldo()).isEqualByComparingTo("4");
        assertThat(n.getValorTotal()).isEqualByComparingTo("12");
        assertThat(
                        jdbc.queryForObject(
                                "select quantidade_kg from negociacao_itens", BigDecimal.class))
                .isEqualByComparingTo("6");
        assertThat(
                        jdbc.queryForObject(
                                "select preco_unitario from negociacao_itens", BigDecimal.class))
                .isEqualByComparingTo("5");
    }

    @Test
    void recusaPersisteJustificativaESincronizaVinculoSemSaida() {
        service.recusar(n.getNegociacaoId(), EMPRESA, "Não atende ao prazo.");
        service.recusar(n.getNegociacaoId(), EMPRESA, "Não atende ao prazo.");
        assertThat(n.getStatus().getStatusAtual()).isEqualTo("Negociação recusada");
        assertThat(n.getDataFechamento()).isNotNull();
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from movimentacoes_estoques", Integer.class))
                .isZero();
        var captor = org.mockito.ArgumentCaptor.forClass(NegociacaoMensagem.class);
        verify(mensagens).save(captor.capture());
        assertThat(captor.getValue().getMensagem()).isEqualTo("Não atende ao prazo.");
    }

    private BigDecimal saldo() {
        return jdbc.queryForObject("select quantidade_kg from estoques", BigDecimal.class);
    }

    public static class SaldoTrigger implements Trigger {
        public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
            try (var statement =
                    conn.prepareStatement(
                            "update estoques set quantidade_kg=quantidade_kg+? where"
                                    + " estoque_id=?")) {
                statement.setObject(1, newRow[3]);
                statement.setObject(2, newRow[1]);
                statement.executeUpdate();
            }
        }
    }
}
