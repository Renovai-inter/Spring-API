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
                "create table cooperativas(cooperativa_id uuid primary key,nome"
                    + " varchar(255),imagem_url text)");
        jdbc.execute(
                "create table empresa_cooperativas_favoritas(favorito_id uuid default random_uuid()"
                    + " primary key,empresa_id uuid,cooperativa_id uuid references"
                    + " cooperativas,data_criacao timestamp default"
                    + " current_timestamp,unique(empresa_id,cooperativa_id))");
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
        doAnswer(
                        inv ->
                                jdbc.update(
                                        "insert into empresa_materiais_interesses values(?,?)",
                                        inv.getArgument(0, UUID.class),
                                        inv.getArgument(1, UUID.class)))
                .when(repository)
                .inserirInteresse(any(), any());
        doAnswer(
                        inv ->
                                jdbc.update(
                                        "insert into"
                                            + " empresa_cooperativas_favoritas(empresa_id,cooperativa_id)"
                                            + " select ?,? where not exists(select 1 from"
                                            + " empresa_cooperativas_favoritas where empresa_id=?"
                                            + " and cooperativa_id=?)",
                                        inv.getArgument(0, UUID.class),
                                        inv.getArgument(1, UUID.class),
                                        inv.getArgument(0, UUID.class),
                                        inv.getArgument(1, UUID.class)))
                .when(repository)
                .adicionarFavorito(any(), any());
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
    void favoritoRepetidoRetornaMesmoRegistroERemocaoRepetidaPreservaOutraEmpresa() {
        UUID cooperativaId = UUID.randomUUID(), outraEmpresaId = UUID.randomUUID();
        jdbc.update(
                "insert into cooperativas(cooperativa_id,nome) values(?,'Cooperativa')",
                cooperativaId);
        var favorito = service.favoritar(EMAIL, cooperativaId);
        var repetido = service.favoritar(EMAIL, cooperativaId);
        assertThat(repetido.favoritoId()).isEqualTo(favorito.favoritoId());
        assertThat(repetido.dataCriacao()).isEqualTo(favorito.dataCriacao());
        assertThat(service.favoritos(EMAIL)).hasSize(1);
        jdbc.update(
                "insert into empresa_cooperativas_favoritas(empresa_id,cooperativa_id) values(?,?)",
                outraEmpresaId,
                cooperativaId);
        service.desfavoritar(EMAIL, cooperativaId);
        service.desfavoritar(EMAIL, cooperativaId);
        assertThat(service.favoritos(EMAIL)).isEmpty();
        assertThat(repository.listarFavoritos(outraEmpresaId)).hasSize(1);
        verify(repository, times(4)).bloquearEmpresa(empresaId);
    }

    @Test
    void favoritoComCooperativaInexistenteNaoAlteraSelecao() {
        assertThatThrownBy(() -> service.favoritar(EMAIL, UUID.randomUUID()))
                .hasMessage("Cooperativa não encontrada.");
        assertThat(service.favoritos(EMAIL)).isEmpty();
    }

    @Test
    void postDeInteresseExigeCategoriaEListaVaziaContinuaValidaNoPut() {
        try (var factory = jakarta.validation.Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(
                            validator.validate(
                                    new com.renovai.api.dto.request.EmpresaContaRequests
                                            .InteresseRequest(null)))
                    .hasSize(1);
            assertThat(
                            validator.validate(
                                    new com.renovai.api.dto.request.EmpresaContaRequests
                                            .SubstituirInteresses(List.of())))
                    .isEmpty();
        }
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
