package com.renovai.api.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.renovai.api.dto.request.Requests.RateioIndividualRequest;
import com.renovai.api.dto.request.Requests.RateioParticipanteRequest;
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
import java.time.LocalDateTime;
import java.util.*;

class RateioIndividualServiceTest {
    private JdbcTemplate jdbc;
    private RateioService service;
    private RateioFuncionarioRepository distribuicoes;
    private ItemRepository itens;
    private Cooperativa cooperativa;
    private Funcionario gestor, primeiro, segundo, terceiro;
    private final Map<UUID, Funcionario> funcionarios = new HashMap<>();
    private static final String EMAIL = "gestor@teste.com";
    private final LocalDateTime inicio = LocalDateTime.of(2026, 9, 1, 0, 0);
    private final LocalDateTime fim = LocalDateTime.of(2026, 9, 30, 23, 59);

    @BeforeEach
    void preparar() {
        var ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("create table cooperativas(cooperativa_id uuid primary key)");
        jdbc.execute(
                "create table rateios(rateio_id uuid primary key,cooperativa_id uuid not null"
                    + " references cooperativas,mes_referencia date not"
                    + " null,unique(cooperativa_id,mes_referencia))");
        jdbc.execute(
                "create table rateios_funcionarios(rateio_funcionario_id uuid default random_uuid()"
                    + " primary key,rateio_id uuid references rateios,cooperado_id uuid not"
                    + " null,valor_rateio numeric(10,2)"
                    + " check(valor_rateio>=0),unique(rateio_id,cooperado_id))");
        cooperativa = new Cooperativa();
        cooperativa.setCooperativaId(UUID.randomUUID());
        cooperativa.setNome("Cooperativa");
        jdbc.update("insert into cooperativas values(?)", cooperativa.getCooperativaId());
        gestor = funcionario("Gestor", EMAIL);
        primeiro = funcionario("Cooperado", "primeiro@teste.com");
        segundo = funcionario("Cooperado", "segundo@teste.com");
        terceiro = funcionario("Cooperado", "terceiro@teste.com");
        var rateios = mock(RateioRepository.class);
        distribuicoes = mock(RateioFuncionarioRepository.class);
        var pessoas = mock(FuncionarioRepository.class);
        var cooperativas = mock(CooperativaRepository.class);
        var tipos = mock(TipoRateioRepository.class);
        var tipo = new TipoRateio();
        tipo.setTipoRateioId(UUID.randomUUID());
        tipo.setTipoRateio("PROPORCIONAL");
        when(tipos.findByTipoRateio("PROPORCIONAL")).thenReturn(Optional.of(tipo));
        when(pessoas.findById(any()))
                .thenAnswer(inv -> Optional.ofNullable(funcionarios.get(inv.getArgument(0))));
        when(cooperativas.buscarComBloqueio(cooperativa.getCooperativaId()))
                .thenAnswer(
                        inv -> {
                            jdbc.queryForObject(
                                    "select cooperativa_id from cooperativas where cooperativa_id=?"
                                        + " for update",
                                    UUID.class,
                                    cooperativa.getCooperativaId());
                            return Optional.of(cooperativa);
                        });
        when(rateios.existsByCooperativa_CooperativaIdAndMesReferencia(any(), any()))
                .thenAnswer(
                        inv ->
                                jdbc.queryForObject(
                                                "select count(*) from rateios where"
                                                    + " cooperativa_id=? and mes_referencia=?",
                                                Integer.class,
                                                inv.getArgument(0),
                                                inv.getArgument(1))
                                        > 0);
        when(rateios.saveAndFlush(any()))
                .thenAnswer(
                        inv -> {
                            Rateio rateio = inv.getArgument(0);
                            rateio.setRateioId(UUID.randomUUID());
                            jdbc.update(
                                    "insert into rateios values(?,?,?)",
                                    rateio.getRateioId(),
                                    rateio.getCooperativa().getCooperativaId(),
                                    rateio.getMesReferencia());
                            return rateio;
                        });
        when(distribuicoes.save(any()))
                .thenAnswer(
                        inv -> {
                            RateioFuncionario participante = inv.getArgument(0);
                            jdbc.update(
                                    "insert into"
                                        + " rateios_funcionarios(rateio_id,cooperado_id,valor_rateio)"
                                        + " values(?,?,?)",
                                    participante.getRateio().getRateioId(),
                                    participante.getCooperado().getFuncionarioId(),
                                    participante.getValorRateio());
                            return participante;
                        });
        itens = mock(ItemRepository.class);
        when(itens.sumValoresPorCooperativaEPeriodo(any(), any(), any()))
                .thenReturn(new BigDecimal("100"));
        var target =
                new RateioService(
                        rateios,
                        distribuicoes,
                        pessoas,
                        cooperativas,
                        tipos,
                        mock(ColetaRepository.class),
                        mock(TriagemRepository.class),
                        itens,
                        mock(PerfilRepository.class));
        var factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(
                new TransactionInterceptor(
                        new DataSourceTransactionManager(ds),
                        new AnnotationTransactionAttributeSource()));
        service = (RateioService) factory.getProxy();
    }

    @Test
    void executaPercentuaisIndividuaisEPersisteValoresNoSchemaExistente() {
        var response =
                service.executarRateioIndividual(
                        request(List.of(participante(primeiro, "70"), participante(segundo, "30"))),
                        EMAIL);
        assertThat(response.valorTotalDistribuido()).isEqualByComparingTo("100");
        assertThat(valor(primeiro)).isEqualByComparingTo("70");
        assertThat(valor(segundo)).isEqualByComparingTo("30");
        assertThat(jdbc.queryForObject("select count(*) from rateios", Integer.class)).isEqualTo(1);
    }

    @Test
    void arredondamentoDistribuiCentavosSemPerderTotal() {
        when(itens.sumValoresPorCooperativaEPeriodo(any(), any(), any()))
                .thenReturn(new BigDecimal("0.05"));
        var response =
                service.executarRateioIndividual(
                        request(
                                List.of(
                                        participante(primeiro, "33.3333"),
                                        participante(segundo, "33.3333"),
                                        participante(terceiro, "33.3334"))),
                        EMAIL);
        assertThat(response.valorTotalDistribuido()).isEqualByComparingTo("0.05");
        assertThat(
                        jdbc.queryForObject(
                                "select sum(valor_rateio) from rateios_funcionarios",
                                BigDecimal.class))
                .isEqualByComparingTo("0.05");
        assertThat(valor(terceiro)).isEqualByComparingTo("0.02");
    }

    @Test
    void somaInvalidaEParticipanteRepetidoNaoGravamRateio() {
        assertThatThrownBy(
                        () ->
                                service.executarRateioIndividual(
                                        request(
                                                List.of(
                                                        participante(primeiro, "70"),
                                                        participante(segundo, "20"))),
                                        EMAIL))
                .hasMessageContaining("100%");
        assertThatThrownBy(
                        () ->
                                service.executarRateioIndividual(
                                        request(
                                                List.of(
                                                        participante(primeiro, "50"),
                                                        participante(primeiro, "50"))),
                                        EMAIL))
                .hasMessageContaining("sem repetições");
        assertThat(jdbc.queryForObject("select count(*) from rateios", Integer.class)).isZero();
    }

    @Test
    void participanteInativoOuDeOutraCooperativaNaoRecebeRateio() {
        segundo.setStatusFuncionario("AFASTADO");
        assertThatThrownBy(
                        () ->
                                service.executarRateioIndividual(
                                        request(
                                                List.of(
                                                        participante(primeiro, "50"),
                                                        participante(segundo, "50"))),
                                        EMAIL))
                .hasMessageContaining("ativos e pertencer");
        segundo.setStatusFuncionario("ATIVO");
        var outra = new Cooperativa();
        outra.setCooperativaId(UUID.randomUUID());
        segundo.setCooperativa(outra);
        assertThatThrownBy(
                        () ->
                                service.executarRateioIndividual(
                                        request(
                                                List.of(
                                                        participante(primeiro, "50"),
                                                        participante(segundo, "50"))),
                                        EMAIL))
                .hasMessageContaining("ativos e pertencer");
        assertThat(jdbc.queryForObject("select count(*) from rateios", Integer.class)).isZero();
    }

    @Test
    void contaAutenticadaNaoPodeUsarGestorDeOutraConta() {
        assertThatThrownBy(
                        () ->
                                service.executarRateioIndividual(
                                        request(List.of(participante(primeiro, "100"))),
                                        "outro@teste.com"))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(jdbc.queryForObject("select count(*) from rateios", Integer.class)).isZero();
    }

    @Test
    void repeticaoDoMesmoMesNaoDuplicaRateio() {
        var request = request(List.of(participante(primeiro, "100")));
        service.executarRateioIndividual(request, EMAIL);
        assertThatThrownBy(() -> service.executarRateioIndividual(request, EMAIL))
                .hasMessageContaining("Já existe rateio");
        assertThat(jdbc.queryForObject("select count(*) from rateios", Integer.class)).isEqualTo(1);
    }

    @Test
    void falhaNaSegundaDistribuicaoReverteRateioEPrimeiroParticipante() {
        doAnswer(
                        inv -> {
                            RateioFuncionario participante = inv.getArgument(0);
                            if (participante
                                    .getCooperado()
                                    .getFuncionarioId()
                                    .equals(segundo.getFuncionarioId()))
                                throw new IllegalStateException("Falha simulada");
                            jdbc.update(
                                    "insert into"
                                        + " rateios_funcionarios(rateio_id,cooperado_id,valor_rateio)"
                                        + " values(?,?,?)",
                                    participante.getRateio().getRateioId(),
                                    participante.getCooperado().getFuncionarioId(),
                                    participante.getValorRateio());
                            return participante;
                        })
                .when(distribuicoes)
                .save(any());
        assertThatThrownBy(
                        () ->
                                service.executarRateioIndividual(
                                        request(
                                                List.of(
                                                        participante(primeiro, "50"),
                                                        participante(segundo, "50"))),
                                        EMAIL))
                .hasMessage("Falha simulada");
        assertThat(jdbc.queryForObject("select count(*) from rateios", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from rateios_funcionarios", Integer.class))
                .isZero();
    }

    @Test
    void periodoInvertidoOuComMaisDeUmMesNaoGravaRateio() {
        var participantes = List.of(participante(primeiro, "100"));
        assertThatThrownBy(
                        () ->
                                service.executarRateioIndividual(
                                        new RateioIndividualRequest(
                                                gestor.getFuncionarioId(),
                                                cooperativa.getCooperativaId(),
                                                null,
                                                fim,
                                                inicio,
                                                participantes),
                                        EMAIL))
                .hasMessageContaining("período válido");
        assertThatThrownBy(
                        () ->
                                service.executarRateioIndividual(
                                        new RateioIndividualRequest(
                                                gestor.getFuncionarioId(),
                                                cooperativa.getCooperativaId(),
                                                null,
                                                inicio,
                                                fim.plusMonths(1),
                                                participantes),
                                        EMAIL))
                .hasMessageContaining("mesmo mês");
    }

    private Funcionario funcionario(String cargoNome, String email) {
        var funcionario = new Funcionario();
        funcionario.setFuncionarioId(UUID.randomUUID());
        funcionario.setCooperativa(cooperativa);
        funcionario.setStatusFuncionario("ATIVO");
        var usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setNome(email);
        funcionario.setUsuario(usuario);
        var cargo = new Cargo();
        cargo.setCargo(cargoNome);
        funcionario.setCargo(cargo);
        funcionarios.put(funcionario.getFuncionarioId(), funcionario);
        return funcionario;
    }

    private RateioParticipanteRequest participante(Funcionario funcionario, String percentual) {
        return new RateioParticipanteRequest(
                funcionario.getFuncionarioId(), new BigDecimal(percentual));
    }

    private RateioIndividualRequest request(List<RateioParticipanteRequest> participantes) {
        return new RateioIndividualRequest(
                gestor.getFuncionarioId(),
                cooperativa.getCooperativaId(),
                null,
                inicio,
                fim,
                participantes);
    }

    private BigDecimal valor(Funcionario funcionario) {
        return jdbc.queryForObject(
                "select valor_rateio from rateios_funcionarios where cooperado_id=?",
                BigDecimal.class,
                funcionario.getFuncionarioId());
    }
}
