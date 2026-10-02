package com.renovai.api.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.renovai.api.dto.response.EmpresaContaResponses.MeuPerfil;
import com.renovai.api.repository.EmpresaSchemaRepository;
import com.renovai.api.security.JwtTokenProvider;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.util.List;
import java.util.UUID;

class EmpresaInteressesTransacaoTest {
    private JdbcTemplate jdbc;
    private EmpresaSchemaService service;
    private EmpresaSchemaRepository repository;
    private final UUID empresaId = UUID.randomUUID(),
            antiga = UUID.randomUUID(),
            primeira = UUID.randomUUID(),
            segunda = UUID.randomUUID();
    private static final String EMAIL = "empresa@teste.com";

    @BeforeEach
    void preparar() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(ds);
        jdbc.execute("create table empresas(empresa_id uuid primary key)");
        jdbc.execute(
                "create table categorias_materiais(categoria_id uuid primary key,nome_categoria"
                        + " varchar(50))");
        jdbc.execute(
                "create table empresa_materiais_interesses(empresa_id uuid,categoria_id uuid"
                        + " references categorias_materiais,unique(empresa_id,categoria_id))");
        jdbc.update("insert into empresas values(?)", empresaId);
        for (UUID c : List.of(antiga, primeira, segunda))
            jdbc.update("insert into categorias_materiais values(?,?)", c, c.toString());
        jdbc.update("insert into empresa_materiais_interesses values(?,?)", empresaId, antiga);
        repository = spy(new EmpresaSchemaRepository(jdbc));
        doReturn(
                        List.of(
                                new MeuPerfil(
                                        UUID.randomUUID(),
                                        empresaId,
                                        "Empresa",
                                        null,
                                        EMAIL,
                                        "cnpj",
                                        null,
                                        null,
                                        null,
                                        null,
                                        null,
                                        null)))
                .when(repository)
                .buscarPerfilPorEmail(EMAIL);
        // H2 não implementa ON CONFLICT(colunas); só esta inserção usa SQL equivalente no teste.
        doAnswer(
                        inv ->
                                jdbc.update(
                                        "insert into empresa_materiais_interesses values(?,?)",
                                        inv.getArgument(0, UUID.class),
                                        inv.getArgument(1, UUID.class)))
                .when(repository)
                .inserirInteresse(any(), any());
        var target =
                new EmpresaSchemaService(
                        repository, mock(PasswordEncoder.class), mock(JwtTokenProvider.class));
        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(
                new TransactionInterceptor(
                        new DataSourceTransactionManager(ds),
                        new AnnotationTransactionAttributeSource()));
        service = (EmpresaSchemaService) factory.getProxy();
    }

    @Test
    void substituiSelecaoCompletaDeduplicandoCategorias() {
        assertThat(service.substituirInteresses(EMAIL, List.of(primeira, segunda, primeira)))
                .extracting(i -> i.categoriaId())
                .containsExactlyInAnyOrder(primeira, segunda);
    }

    @Test
    void categoriaInexistentePreservaSelecaoAnterior() {
        assertThatThrownBy(
                        () ->
                                service.substituirInteresses(
                                        EMAIL, List.of(primeira, UUID.randomUUID())))
                .hasMessage("Categoria não encontrada.");
        assertThat(categorias()).containsExactly(antiga);
    }

    @Test
    void falhaNaSegundaInsercaoReverteLimpezaEPrimeiraInsercao() {
        doThrow(new IllegalStateException("Falha simulada"))
                .when(repository)
                .inserirInteresse(empresaId, segunda);
        assertThatThrownBy(() -> service.substituirInteresses(EMAIL, List.of(primeira, segunda)))
                .hasMessage("Falha simulada");
        assertThat(categorias()).containsExactly(antiga);
    }

    @Test
    void removeCategoriaIndividualPreservandoAsDemais() {
        service.substituirInteresses(EMAIL, List.of(primeira, segunda));
        service.removerInteresse(EMAIL, primeira);
        service.removerInteresse(EMAIL, primeira);
        assertThat(categorias()).containsExactly(segunda);
    }

    @Test
    void listaVaziaLimpaSelecaoNaMesmaTransacao() {
        assertThat(service.substituirInteresses(EMAIL, List.of())).isEmpty();
        assertThat(categorias()).isEmpty();
    }

    private List<UUID> categorias() {
        return jdbc.query(
                "select categoria_id from empresa_materiais_interesses where empresa_id=?",
                (r, n) -> r.getObject(1, UUID.class),
                empresaId);
    }
}
