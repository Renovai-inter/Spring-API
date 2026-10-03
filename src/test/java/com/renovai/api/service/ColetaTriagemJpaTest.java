package com.renovai.api.service;

import static org.assertj.core.api.Assertions.*;

import com.renovai.api.dto.request.Requests.*;
import com.renovai.api.model.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.List;

@DataJpaTest(showSql = false, properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Import({ColetaService.class, TriagemService.class, TriagemEstoqueService.class})
class ColetaTriagemJpaTest {
    @Autowired private TestEntityManager entityManager;
    @Autowired private ColetaService coletas;
    @Autowired private TriagemService triagens;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private com.renovai.api.repository.TriagemRepository triagemRepository;
    private Funcionario cooperado;
    private Equipe equipe;
    private Material material;

    @BeforeEach
    void preparar() {
        jdbc.execute(
                "alter table movimentacoes_estoques alter column movimentacao_id set default"
                        + " random_uuid()");
        jdbc.execute(
                "alter table movimentacoes_estoques alter column data_movimentacao set default"
                        + " current_timestamp");
        jdbc.execute(
                "alter table estoques alter column data_atualizacao set default current_timestamp");
        var cooperativa = new Cooperativa();
        cooperativa.setNome("Cooperativa");
        entityManager.persist(cooperativa);
        var cargo = new Cargo();
        cargo.setCargo("Gestor");
        entityManager.persist(cargo);
        var usuario = new Usuario();
        usuario.setNome("Gestor");
        usuario.setEmail("gestor@teste.com");
        usuario.setCpf("12345678901");
        usuario.setSenhaHash("hash");
        entityManager.persist(usuario);
        cooperado = new Funcionario();
        cooperado.setUsuario(usuario);
        cooperado.setCooperativa(cooperativa);
        cooperado.setCargo(cargo);
        entityManager.persist(cooperado);
        equipe = new Equipe();
        equipe.setNome("Equipe");
        equipe.setGestor(cooperado);
        entityManager.persist(equipe);
        var vinculo = new EquipeCooperado();
        vinculo.setEquipe(equipe);
        vinculo.setCooperado(cooperado);
        entityManager.persist(vinculo);
        var categoria = new CategoriaMaterial();
        categoria.setNomeCategoria("Papel");
        entityManager.persist(categoria);
        material = new Material();
        material.setCategoria(categoria);
        entityManager.persist(material);
        for (String nome : List.of("Em triagem", "Concluído")) {
            var status = new Status();
            status.setReferencia("TRIAGEM");
            status.setStatusAtual(nome);
            entityManager.persist(status);
        }
        entityManager.flush();
    }

    @Test
    void materialSeparadoPersisteColetaTriagemEEstoqueComRepositoriosReais() {
        var response = coletas.criar(request(false));
        entityManager.clear();
        var consultada = coletas.buscarPorId(response.coletaId());
        assertThat(consultada.precisaTriagem()).isFalse();
        assertThat(consultada.materiais()).hasSize(1);
        assertThat(
                        jdbc.queryForObject(
                                "select quantidade_kg from estoques where material_id=?",
                                BigDecimal.class,
                                material.getMaterialId()))
                .isEqualByComparingTo("100");
    }

    @Test
    void materialPendenteSoEntraNaConclusaoEConsultaMantemEstado() {
        var response = coletas.criar(request(true));
        entityManager.clear();
        assertThat(coletas.buscarPorId(response.coletaId()).precisaTriagem()).isTrue();
        assertThat(triagemRepository.findAbertysByCooperado(cooperado.getFuncionarioId()))
                .hasSize(1);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from movimentacoes_estoques", Integer.class))
                .isZero();
        triagens.concluir(
                response.materiais().get(0).triagemId(),
                new ConcluirTriagemRequest(new BigDecimal("80"), null));
        entityManager.clear();
        triagens.concluir(
                response.materiais().get(0).triagemId(),
                new ConcluirTriagemRequest(new BigDecimal("80"), null));
        entityManager.clear();
        assertThat(coletas.buscarPorId(response.coletaId()).precisaTriagem()).isFalse();
        assertThat(triagemRepository.findAbertysByCooperado(cooperado.getFuncionarioId()))
                .isEmpty();
        assertThat(
                        jdbc.queryForObject(
                                "select quantidade_kg from estoques where material_id=?",
                                BigDecimal.class,
                                material.getMaterialId()))
                .isEqualByComparingTo("80");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from movimentacoes_estoques", Integer.class))
                .isEqualTo(1);
    }

    private ColetaRequest request(boolean precisa) {
        return new ColetaRequest(
                cooperado.getFuncionarioId(),
                null,
                new BigDecimal("100"),
                null,
                "INTERNA",
                null,
                equipe.getEquipeId(),
                precisa,
                List.of(
                        new ColetaMaterialRequest(
                                material.getMaterialId(), new BigDecimal("100"))));
    }
}
