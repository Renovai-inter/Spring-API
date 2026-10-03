package com.renovai.api.service;

import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.*;
import com.renovai.api.repository.*;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class NegociacaoFluxoService {
    private final NegociacaoRepository negociacoes;
    private final StatusRepository statuses;
    private final PerfilRepository perfis;
    private final UsuarioRepository usuarios;
    private final FuncionarioRepository funcionarios;
    private final NegociacaoMensagemRepository mensagens;
    private final JdbcTemplate jdbc;

    public NegociacaoFluxoService(
            NegociacaoRepository negociacoes,
            StatusRepository statuses,
            PerfilRepository perfis,
            UsuarioRepository usuarios,
            FuncionarioRepository funcionarios,
            NegociacaoMensagemRepository mensagens,
            JdbcTemplate jdbc) {
        this.negociacoes = negociacoes;
        this.statuses = statuses;
        this.perfis = perfis;
        this.usuarios = usuarios;
        this.funcionarios = funcionarios;
        this.mensagens = mensagens;
        this.jdbc = jdbc;
    }

    public Negociacao bloquear(UUID id) {
        List<UUID> pedidos =
                jdbc.query(
                        "select pedido_id from negociacoes where negociacao_id=?",
                        (r, n) -> r.getObject(1, UUID.class),
                        id);
        if (pedidos.isEmpty())
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Negociação não encontrada.");
        jdbc.queryForObject(
                "select pedido_id from pedidos where pedido_id=? for update",
                UUID.class,
                pedidos.get(0));
        return negociacoes
                .buscarComBloqueio(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public Perfil participante(Negociacao n, String email) {
        Perfil perfil =
                perfis.findByEmailIgnoreCase(email)
                        .filter(p -> Boolean.TRUE.equals(p.getEstaAtivo()))
                        .orElse(null);
        if (perfil != null
                && ((perfil.getEmpresa() != null
                                && perfil.getEmpresa()
                                        .getEmpresaId()
                                        .equals(n.getEmpresa().getEmpresaId()))
                        || (perfil.getCooperativa() != null
                                && perfil.getCooperativa()
                                        .getCooperativaId()
                                        .equals(n.getCooperativa().getCooperativaId()))))
            return perfil;
        if (gestorDaCooperativa(n, email)) {
            return perfis.findByCooperativa_CooperativaIdAtivo(
                            n.getCooperativa().getCooperativaId())
                    .orElseThrow(
                            () ->
                                    new RegraDeNegocioException(
                                            "Perfil institucional da cooperativa não encontrado."));
        }
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN, "A negociação não pertence à sua conta.");
    }

    private boolean gestorDaCooperativa(Negociacao n, String email) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || auth.getAuthorities().stream()
                        .noneMatch(
                                a ->
                                        Set.of("ROLE_GESTOR_COOPERATIVA", "ROLE_ADMIN_COOPERATIVA")
                                                .contains(a.getAuthority()))) return false;
        return usuarios.findByEmail(email)
                .flatMap(funcionarios::findByUsuario)
                .filter(f -> "ATIVO".equals(f.getStatusFuncionario()))
                .filter(
                        f ->
                                f.getCooperativa() != null
                                        && f.getCooperativa()
                                                .getCooperativaId()
                                                .equals(n.getCooperativa().getCooperativaId()))
                .isPresent();
    }

    public void exigirGestor(Negociacao n, String email) {
        Perfil perfil = participante(n, email);
        if (perfil.getCooperativa() == null
                || !perfil.getCooperativa()
                        .getCooperativaId()
                        .equals(n.getCooperativa().getCooperativaId()))
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Somente o gestor da cooperativa pode concluir o pedido.");
    }

    public void exigirAberta(Negociacao n) {
        if (n.getDataFechamento() != null
                || !Set.of("EM_ANDAMENTO", "EM_NEGOCIACAO", "ABERTO")
                        .contains(normalizar(n.getStatus().getStatusAtual())))
            throw new RegraDeNegocioException("A negociação não está aberta para alteração.");
    }

    public Status status(String referencia, String... nomes) {
        List<Status> existentes = statuses.findByReferencia(referencia);
        for (String nome : nomes) {
            Optional<Status> encontrado =
                    existentes.stream()
                            .filter(s -> normalizar(s.getStatusAtual()).equals(normalizar(nome)))
                            .findFirst();
            if (encontrado.isPresent()) return encontrado.get();
        }
        throw new RegraDeNegocioException(
                "Status de " + referencia + " não cadastrado: " + String.join(" / ", nomes));
    }

    public void sincronizarVinculo(Negociacao n, Status status) {
        int alterados =
                jdbc.update(
                        "update pedidos_cooperativas set status_id=? where pedido_id=? and"
                                + " cooperativa_id=?",
                        status.getStatusId(),
                        n.getPedido().getPedidoId(),
                        n.getCooperativa().getCooperativaId());
        if (alterados != 1)
            throw new RegraDeNegocioException(
                    "Vínculo do pedido com a cooperativa não encontrado.");
    }

    public void validarAbertura(Negociacao n) {
        jdbc.queryForObject(
                "select pedido_id from pedidos where pedido_id=? for update",
                UUID.class,
                n.getPedido().getPedidoId());
        List<String> atuais =
                jdbc.query(
                        """
                        select s.status_atual from pedidos_cooperativas pc join status s on s.status_id=pc.status_id
                        where pc.pedido_id=? and pc.cooperativa_id=? for update
                        """,
                        (r, i) -> r.getString(1),
                        n.getPedido().getPedidoId(),
                        n.getCooperativa().getCooperativaId());
        if (atuais.size() != 1
                || !Set.of("ABERTO", "EM_NEGOCIACAO").contains(normalizar(atuais.get(0))))
            throw new RegraDeNegocioException(
                    "Somente pedidos abertos podem iniciar uma negociação.");
    }

    public void registrarObservacao(Negociacao n, String email, String observacao, String tipo) {
        Perfil remetente = participante(n, email);
        NegociacaoMensagem mensagem = new NegociacaoMensagem();
        mensagem.setNegociacao(n);
        mensagem.setRemetente(remetente);
        mensagem.setMensagem(observacao == null ? "" : observacao);
        mensagem.setTipoMensagem(tipo);
        mensagens.save(mensagem);
    }

    private record ItemAcordo(UUID materialId, BigDecimal quantidade, BigDecimal preco) {}

    private record ItemPedido(UUID itemId, UUID materialId) {}

    private record Saldo(UUID estoqueId, BigDecimal quantidade) {}

    public Negociacao aceitar(UUID id, String email) {
        Negociacao n = bloquear(id);
        Perfil perfil = participante(n, email);
        if (perfil.getEmpresa() == null)
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Somente a Empresa pode aceitar a contraproposta.");
        String atual = normalizar(n.getStatus().getStatusAtual());
        if (Set.of("ACORDO_FECHADO", "ACEITO", "CONCLUIDO").contains(atual)) return n;
        exigirAberta(n);
        Status aceito = status("NEGOCIACAO", "Acordo fechado", "ACEITO");
        Status pedidoAceito = status("PEDIDO", "Aceito");
        List<ItemAcordo> itens =
                jdbc.query(
                        "select material_id,quantidade_kg,preco_unitario from negociacao_itens"
                                + " where negociacao_id=? order by material_id",
                        (r, i) ->
                                new ItemAcordo(
                                        r.getObject(1, UUID.class),
                                        r.getBigDecimal(2),
                                        r.getBigDecimal(3)),
                        id);
        boolean usarItensPedido = itens.isEmpty();
        if (usarItensPedido) {
            itens =
                    jdbc.query(
                            """
                            select material_id,case when count(quantidade_kg)=count(*) then sum(quantidade_kg) else null end,
                            sum(quantidade_kg*preco_unitario)/nullif(sum(quantidade_kg),0)
                            from pedido_itens where pedido_id=? group by material_id order by material_id
                            """,
                            (r, i) ->
                                    new ItemAcordo(
                                            r.getObject(1, UUID.class),
                                            r.getBigDecimal(2),
                                            r.getBigDecimal(3)),
                            n.getPedido().getPedidoId());
        }
        if (itens.isEmpty())
            throw new RegraDeNegocioException("O pedido precisa ter itens para o aceite.");
        List<ItemPedido> originais =
                jdbc.query(
                        "select item_id,material_id from pedido_itens where pedido_id=? order by"
                                + " item_id",
                        (r, i) ->
                                new ItemPedido(
                                        r.getObject(1, UUID.class), r.getObject(2, UUID.class)),
                        n.getPedido().getPedidoId());
        BigDecimal total = BigDecimal.ZERO;
        for (ItemAcordo item : itens) {
            if (item.quantidade() == null
                    || item.quantidade().signum() <= 0
                    || item.preco() == null
                    || item.preco().signum() < 0)
                throw new RegraDeNegocioException("Peso e preço da contraproposta inválidos.");
            UUID itemId =
                    originais.stream()
                            .filter(i -> i.materialId().equals(item.materialId()))
                            .map(ItemPedido::itemId)
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new RegraDeNegocioException(
                                                    "O material negociado não pertence ao"
                                                            + " pedido."));
            List<Saldo> saldos =
                    jdbc.query(
                            "select estoque_id,quantidade_kg from estoques where cooperativa_id=?"
                                    + " and material_id=? for update",
                            (r, i) -> new Saldo(r.getObject(1, UUID.class), r.getBigDecimal(2)),
                            n.getCooperativa().getCooperativaId(),
                            item.materialId());
            if (saldos.size() != 1 || saldos.get(0).quantidade().compareTo(item.quantidade()) < 0)
                throw new RegraDeNegocioException(
                        "Estoque insuficiente para aceitar a contraproposta.");
            Saldo saldo = saldos.get(0);
            Long movimentos =
                    jdbc.queryForObject(
                            """
                            select count(*) from movimentacoes_estoques me join pedido_itens pi on pi.item_id=me.item_id
                            where me.estoque_id=? and pi.pedido_id=?
                            """,
                            Long.class,
                            saldo.estoqueId(),
                            n.getPedido().getPedidoId());
            if (movimentos != null && movimentos > 0)
                throw new RegraDeNegocioException(
                        "Este pedido já possui saída de estoque para o material.");
            if (usarItensPedido) {
                jdbc.update(
                        "insert into"
                            + " negociacao_itens(negociacao_id,material_id,quantidade_kg,preco_unitario)"
                            + " values(?,?,?,?)",
                        id,
                        item.materialId(),
                        item.quantidade(),
                        item.preco().setScale(2, java.math.RoundingMode.HALF_UP));
            }
            jdbc.update(
                    "insert into"
                        + " movimentacoes_estoques(estoque_id,item_id,quantidade_kg,tipo_movimentacao)"
                        + " values(?,?,?,'SAIDA')",
                    saldo.estoqueId(),
                    itemId,
                    item.quantidade().negate());
            BigDecimal esperado = saldo.quantidade().subtract(item.quantidade());
            BigDecimal depois =
                    jdbc.queryForObject(
                            "select quantidade_kg from estoques where estoque_id=?",
                            BigDecimal.class,
                            saldo.estoqueId());
            if (depois != null && depois.compareTo(saldo.quantidade()) == 0) {
                jdbc.update(
                        "update estoques set quantidade_kg=?,data_atualizacao=now() where"
                                + " estoque_id=?",
                        esperado,
                        saldo.estoqueId());
            } else if (depois == null || depois.compareTo(esperado) != 0) {
                throw new RegraDeNegocioException(
                        "Saldo divergente após registrar a saída de estoque.");
            }
            total =
                    total.add(
                            item.quantidade()
                                    .multiply(item.preco())
                                    .setScale(2, java.math.RoundingMode.HALF_UP));
        }
        if (n.getValorTotal() == null) n.setValorTotal(total);
        n.setStatus(aceito);
        negociacoes.saveAndFlush(n);
        sincronizarVinculo(n, pedidoAceito);
        return n;
    }

    public Negociacao concluir(UUID id, String email, BigDecimal valorFinal, String observacao) {
        Negociacao n = bloquear(id);
        exigirGestor(n, email);
        if (!Set.of("ACORDO_FECHADO", "ACEITO", "CONCLUIDO")
                .contains(normalizar(n.getStatus().getStatusAtual())))
            throw new RegraDeNegocioException(
                    "A Empresa precisa aceitar a contraproposta antes da conclusão.");
        if (valorFinal != null
                && (n.getValorTotal() == null || valorFinal.compareTo(n.getValorTotal()) != 0))
            throw new RegraDeNegocioException(
                    "O valor final deve manter o valor aceito pela Empresa.");
        if (n.getDataFechamento() != null) return n;
        n.setDataFechamento(LocalDateTime.now());
        negociacoes.saveAndFlush(n);
        sincronizarVinculo(n, status("PEDIDO", "Finalizado", "Concluído", "CONCLUIDO"));
        atualizarConclusaoPedido(n);
        if (observacao != null && !observacao.isBlank())
            registrarObservacao(n, email, observacao, "SISTEMA");
        return n;
    }

    private void atualizarConclusaoPedido(Negociacao n) {
        jdbc.update(
                """
                update pedidos p set data_conclusao=now() where p.pedido_id=? and p.data_conclusao is null and not exists(
                select 1 from pedidos_cooperativas pc join status s on s.status_id=pc.status_id
                where pc.pedido_id=p.pedido_id and lower(s.status_atual) not in ('finalizado','concluído','concluido','recusado','cancelado'))
                """,
                n.getPedido().getPedidoId());
    }

    public Negociacao recusar(UUID id, String email, String justificativa) {
        Negociacao n = bloquear(id);
        participante(n, email);
        if (Set.of("NEGOCIACAO_RECUSADA", "RECUSADO")
                .contains(normalizar(n.getStatus().getStatusAtual()))) return n;
        exigirAberta(n);
        n.setStatus(status("NEGOCIACAO", "Negociação recusada", "RECUSADO"));
        n.setDataFechamento(LocalDateTime.now());
        negociacoes.saveAndFlush(n);
        sincronizarVinculo(n, status("PEDIDO", "Recusado"));
        atualizarConclusaoPedido(n);
        registrarObservacao(n, email, justificativa, "SISTEMA");
        return n;
    }

    public static String normalizar(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toUpperCase(Locale.ROOT)
                .replace(' ', '_');
    }
}
