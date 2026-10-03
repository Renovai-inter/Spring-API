package com.renovai.api.repository;

import com.renovai.api.dto.response.EmpresaContaResponses.AvaliacaoPublica;
import com.renovai.api.dto.response.EmpresaContaResponses.MeuPerfil;
import com.renovai.api.dto.response.EmpresaContaResponses.PerfilPublico;
import com.renovai.api.dto.response.Responses.CooperativaResponse;
import com.renovai.api.dto.response.Responses.EstoqueResponse;
import com.renovai.api.dto.response.Responses.ItemResponse;
import com.renovai.api.dto.response.Responses.MaterialResponse;
import com.renovai.api.dto.response.Responses.PedidoCooperativaResponse;
import com.renovai.api.dto.response.Responses.PedidoResponse;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Consultas parametrizadas da conta de empresa para o schema PostgreSQL fornecido. */
@Repository
public class EmpresaSchemaRepository {
    private final JdbcTemplate jdbcTemplate;

    public record CredencialEmpresa(UUID perfilId, String senhaHash) {}

    public EmpresaSchemaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String PERFIL =
"""
select
p.perfil_id,p.empresa_id,p.email,p.cnpj,p.endereco_id,e.nome,e.descricao,en.logradouro,en.cidade,
(select concat_ws('',t.ddd,t.telefone) from telefones t where t.perfil_id=p.perfil_id order by
t.telefone_id limit 1) telefone from perfis p join empresas e on e.empresa_id=p.empresa_id left join
enderecos en on en.endereco_id=p.endereco_id where lower(p.email)=lower(?)
and p.esta_ativo=true
""";
    private static final String COOP =
"""
select c.cooperativa_id,c.nome,c.descricao,c.numero_cooperados,c.horario_funcionamento,(select
en.cidade from perfis p left join enderecos en on en.endereco_id=p.endereco_id where
p.cooperativa_id=c.cooperativa_id and p.esta_ativo=true order by p.data_criacao,p.perfil_id limit 1)
cidade from cooperativas c
""";
    private static final String PEDIDOS =
"""
select p.pedido_id,p.empresa_id,e.nome,p.data_pedido,p.data_conclusao,p.observacao,(select s.status_atual from
pedidos_cooperativas pc join status s on s.status_id=pc.status_id where pc.pedido_id=p.pedido_id
order by case when lower(s.status_atual) in ('aceito','finalizado','concluído','concluido') then 0
when lower(s.status_atual) in ('recusado','cancelado') then 2 else 1 end,pc.pedido_cooperativa_id
limit 1) status_atual,coalesce((select sum(coalesce(n.valor_total,
(select sum(i.quantidade_kg*i.preco_unitario) from pedido_itens i where i.pedido_id=p.pedido_id)))
from pedidos_cooperativas pc join status s on s.status_id=pc.status_id
left join negociacoes n on n.pedido_id=pc.pedido_id and n.cooperativa_id=pc.cooperativa_id
where pc.pedido_id=p.pedido_id and lower(s.status_atual) in ('aceito','finalizado','concluído','concluido')),
(select sum(i.quantidade_kg*coalesce(i.preco_unitario,0)) from pedido_itens
i where i.pedido_id=p.pedido_id),0) valor_total from pedidos p join empresas e on
e.empresa_id=p.empresa_id
""";
    private static final String AV =
"""
select
a.avaliacao_id,a.avaliador_id,a.avaliado_id,a.pedido_id,a.nota,a.comentario,a.data_avaliacao,e.nome
avaliador_nome from avaliacoes a join perfis pa on pa.perfil_id=a.avaliador_id left join empresas e
on e.empresa_id=pa.empresa_id join perfis pv on pv.perfil_id=a.avaliado_id
""";
    private final RowMapper<MeuPerfil> perfilMapper =
            (r, n) ->
                    new MeuPerfil(
                            uuid(r, "perfil_id"),
                            uuid(r, "empresa_id"),
                            r.getString("nome"),
                            r.getString("descricao"),
                            r.getString("email"),
                            r.getString("cnpj"),
                            r.getString("logradouro"),
                            r.getString("telefone"),
                            r.getString("cidade"),
                            null,
                            null,
                            null);

    private final RowMapper<PedidoResponse> pedidoMapper =
            (r, n) ->
                    new PedidoResponse(
                            uuid(r, "pedido_id"),
                            uuid(r, "empresa_id"),
                            r.getString("nome"),
                            data(r, "data_pedido"),
                            data(r, "data_conclusao"),
                            r.getString("status_atual"),
                            r.getBigDecimal("valor_total"),
                            r.getString("observacao"));

    private final RowMapper<AvaliacaoPublica> avaliacaoMapper =
            (r, n) ->
                    new AvaliacaoPublica(
                            uuid(r, "avaliacao_id"),
                            uuid(r, "avaliador_id"),
                            r.getString("avaliador_nome"),
                            uuid(r, "avaliado_id"),
                            uuid(r, "pedido_id"),
                            r.getObject("nota", Integer.class),
                            r.getString("comentario"),
                            data(r, "data_avaliacao").toString());

    private static UUID uuid(ResultSet r, String c) throws SQLException {
        return r.getObject(c, UUID.class);
    }

    private static LocalDateTime data(ResultSet r, String c) throws SQLException {
        Timestamp t = r.getTimestamp(c);
        return t == null ? null : t.toLocalDateTime();
    }

    public Boolean existeEmpresaPorEmail(String email) {
        return jdbcTemplate.queryForObject(
"""
select exists(select 1 from perfis where lower(email)=lower(?) and empresa_id is not null)
""",
                Boolean.class,
                email);
    }

    public List<String> buscarHashesPorEmail(String email) {
        return jdbcTemplate.query(
"""
select senha_hash from perfis where lower(email)=lower(?) and empresa_id is not null and esta_ativo=true
""",
                (r, n) -> r.getString(1),
                email);
    }

    public List<CredencialEmpresa> buscarCredenciaisPorEmail(String email) {
        return jdbcTemplate.query(
                """
                select perfil_id, senha_hash from perfis where lower(email) = lower(?) and empresa_id is not null and esta_ativo=true
                """,
                (rs, rowNum) ->
                        new CredencialEmpresa(uuid(rs, "perfil_id"), rs.getString("senha_hash")),
                email);
    }

    public Long contarEmailsCadastrados(String email) {
        return jdbcTemplate.queryForObject(
"""
select (select count(*) from usuarios where lower(email)=lower(?))+(select count(*) from perfis
where lower(email)=lower(?))
""",
                Long.class,
                email,
                email);
    }

    public Boolean existeCpf(String cpf) {
        return jdbcTemplate.queryForObject(
"""
select exists(select 1 from usuarios where regexp_replace(cpf,'[^0-9]','','g')=?)
""",
                Boolean.class,
                cpf);
    }

    public int inserirUsuario(
            UUID usuarioId, String nome, String email, String cpf, String senhaHash) {
        return jdbcTemplate.update(
"""
insert into usuarios(usuario_id,nome,email,cpf,senha_hash) values(?,?,?,?,?)
""",
                usuarioId,
                nome,
                email,
                cpf,
                senhaHash);
    }

    public int inserirEmpresa(UUID empresaId, String nome) {
        return jdbcTemplate.update(
"""
insert into empresas(empresa_id,nome) values(?,?)
""",
                empresaId,
                nome);
    }

    public int inserirEndereco(UUID enderecoId, String logradouro) {
        return jdbcTemplate.update(
"""
insert into enderecos(endereco_id,logradouro) values(?,?)
""",
                enderecoId,
                logradouro);
    }

    public int inserirPerfil(
            UUID perfilId,
            UUID empresaId,
            UUID enderecoId,
            String email,
            String cnpj,
            String senhaHash) {
        return jdbcTemplate.update(
"""
insert into perfis(perfil_id,empresa_id,endereco_id,email,cnpj,senha_hash) values(?,?,?,?,?,?)
""",
                perfilId,
                empresaId,
                enderecoId,
                email,
                cnpj,
                senhaHash);
    }

    public int inserirTelefone(UUID telefoneId, UUID perfilId, String telefone) {
        return jdbcTemplate.update(
"""
insert into telefones(telefone_id,perfil_id,telefone) values(?,?,?)
""",
                telefoneId,
                perfilId,
                telefone);
    }

    public List<MeuPerfil> buscarPerfilPorEmail(String email) {
        return jdbcTemplate.query(PERFIL, perfilMapper, email);
    }

    public int atualizarNomeEmpresa(String nome, UUID empresaId) {
        return jdbcTemplate.update(
"""
update empresas set nome=? where empresa_id=?
""",
                nome,
                empresaId);
    }

    public int atualizarDescricaoEmpresa(String descricao, UUID empresaId) {
        return jdbcTemplate.update(
"""
update empresas set descricao=? where empresa_id=?
""",
                descricao,
                empresaId);
    }

    public int atualizarCnpj(String cnpj, UUID perfilId) {
        return jdbcTemplate.update(
"""
update perfis set cnpj=? where perfil_id=?
""",
                cnpj,
                perfilId);
    }

    public UUID buscarEnderecoId(UUID perfilId) {
        return jdbcTemplate.queryForObject(
"""
select endereco_id from perfis where perfil_id=?
""",
                UUID.class,
                perfilId);
    }

    public int inserirEnderecoVazio(UUID enderecoId) {
        return jdbcTemplate.update(
"""
insert into enderecos(endereco_id) values(?)
""",
                enderecoId);
    }

    public int vincularEndereco(UUID enderecoId, UUID perfilId) {
        return jdbcTemplate.update(
"""
update perfis set endereco_id=? where perfil_id=?
""",
                enderecoId,
                perfilId);
    }

    public int atualizarLogradouro(String logradouro, UUID enderecoId) {
        return jdbcTemplate.update(
"""
update enderecos set logradouro=? where endereco_id=?
""",
                logradouro,
                enderecoId);
    }

    public int atualizarCidade(String cidade, UUID enderecoId) {
        return jdbcTemplate.update(
"""
update enderecos set cidade=? where endereco_id=?
""",
                cidade,
                enderecoId);
    }

    public List<UUID> buscarTelefoneIds(UUID perfilId) {
        return jdbcTemplate.query(
"""
select telefone_id from telefones where perfil_id=? order by telefone_id limit 1
""",
                (r, n) -> r.getObject(1, UUID.class),
                perfilId);
    }

    public int atualizarTelefone(String telefone, UUID telefoneId) {
        return jdbcTemplate.update(
"""
update telefones set ddd=null,telefone=? where telefone_id=?
""",
                telefone,
                telefoneId);
    }

    public Boolean existeEmail(String email) {
        return jdbcTemplate.queryForObject(
"""
select exists(select 1 from usuarios where lower(email)=lower(?)) or exists(select 1 from perfis
where lower(email)=lower(?))
""",
                Boolean.class,
                email,
                email);
    }

    public int atualizarEmailUsuario(String novoEmail, String emailAtual) {
        return jdbcTemplate.update(
"""
update usuarios set email=? where lower(email)=lower(?)
""",
                novoEmail,
                emailAtual);
    }

    public int atualizarEmailPerfil(String email, UUID perfilId) {
        return jdbcTemplate.update(
"""
update perfis set email=? where perfil_id=?
""",
                email,
                perfilId);
    }

    public int atualizarSenha(String senhaHash, String email) {
        return jdbcTemplate.update(
"""
update perfis set senha_hash=?,token_redefinicao=null,data_token_expiracao=null where lower(email)=lower(?) and empresa_id is not null and esta_ativo=true
""",
                senhaHash,
                email);
    }

    public List<MaterialResponse> listarMateriaisGlobais() {
        return jdbcTemplate.query(
"""
select m.material_id,m.categoria_id,c.nome_categoria,m.preco_sugerido,m.esta_disponivel from
materiais m join categorias_materiais c on c.categoria_id=m.categoria_id where m.cooperativa_id is
null and m.esta_disponivel=true order by c.nome_categoria,m.material_id
""",
                (r, n) ->
                        new MaterialResponse(
                                uuid(r, "material_id"),
                                uuid(r, "categoria_id"),
                                r.getString("nome_categoria"),
                                r.getBigDecimal("preco_sugerido"),
                                r.getBoolean("esta_disponivel"),
                                null,
                                null));
    }

    public Boolean existeMaterialGlobalDisponivel(UUID materialId) {
        return jdbcTemplate.queryForObject(
"""
select exists(select 1 from materiais where material_id=? and cooperativa_id is null and
esta_disponivel=true)
""",
                Boolean.class,
                materialId);
    }

    public int inserirInteresse(UUID empresaId, UUID categoriaId) {
        return jdbcTemplate.update(
                """
                insert into empresa_materiais_interesses(empresa_material_id,empresa_id,categoria_id) values(gen_random_uuid(),?,?)
                on conflict (empresa_id,categoria_id) do nothing
                """,
                empresaId,
                categoriaId);
    }

    public int limparInteresses(UUID empresaId) {
        return jdbcTemplate.update(
                "delete from empresa_materiais_interesses where empresa_id=?", empresaId);
    }

    public int removerInteresse(UUID empresaId, UUID categoriaId) {
        return jdbcTemplate.update("delete from empresa_materiais_interesses where empresa_id=? and categoria_id=?", empresaId, categoriaId);
    }

    public List<com.renovai.api.dto.response.Responses.FavoritoResponse> listarFavoritos(UUID empresaId) {
        return jdbcTemplate.query("""
            select f.favorito_id,f.cooperativa_id,c.nome,c.imagem_url,f.data_criacao
            from empresa_cooperativas_favoritas f join cooperativas c on c.cooperativa_id=f.cooperativa_id
            where f.empresa_id=? order by f.data_criacao,f.favorito_id
            """, (r,n) -> new com.renovai.api.dto.response.Responses.FavoritoResponse(
                uuid(r,"favorito_id"),empresaId,uuid(r,"cooperativa_id"),r.getString("nome"),r.getString("imagem_url"),data(r,"data_criacao")), empresaId);
    }

    public int adicionarFavorito(UUID empresaId, UUID cooperativaId) {
        return jdbcTemplate.update("insert into empresa_cooperativas_favoritas(empresa_id,cooperativa_id) values(?,?) on conflict (empresa_id,cooperativa_id) do nothing", empresaId, cooperativaId);
    }

    public int removerFavorito(UUID empresaId, UUID cooperativaId) {
        return jdbcTemplate.update("delete from empresa_cooperativas_favoritas where empresa_id=? and cooperativa_id=?", empresaId, cooperativaId);
    }

    public List<com.renovai.api.dto.response.EmpresaContaResponses.Interesse> listarInteresses(
            UUID empresaId) {
        return jdbcTemplate.query(
                """
                select i.empresa_id,i.categoria_id,c.nome_categoria
                from empresa_materiais_interesses i join categorias_materiais c on c.categoria_id=i.categoria_id
                where i.empresa_id=? order by c.nome_categoria,i.categoria_id
                """,
                (r, n) ->
                        new com.renovai.api.dto.response.EmpresaContaResponses.Interesse(
                                uuid(r, "empresa_id"),
                                uuid(r, "categoria_id"),
                                r.getString("nome_categoria")),
                empresaId);
    }

    public List<String> buscarCategoria(UUID categoriaId) {
        return jdbcTemplate.query(
                "select nome_categoria from categorias_materiais where categoria_id=?",
                (r, n) -> r.getString(1),
                categoriaId);
    }

    public List<UUID> buscarCategoriaMaterial(UUID materialId) {
        return jdbcTemplate.query(
                "select categoria_id from materiais where material_id=?",
                (r, n) -> r.getObject(1, UUID.class),
                materialId);
    }

    public List<CooperativaResponse> buscarCooperativas(
            BigDecimal quantidadeMin, UUID categoriaId, String cidade) {
        return jdbcTemplate.query(
                COOP
                        +
"""
 where exists(select 1 from estoques e join materiais m on m.material_id=e.material_id where
e.cooperativa_id=c.cooperativa_id and m.cooperativa_id is null and m.esta_disponivel=true and
e.quantidade_kg>0 and e.quantidade_kg>=? and (cast(? as uuid) is null or m.categoria_id=cast(? as
uuid))) and (cast(? as text) is null or exists(select 1 from perfis p join enderecos en on
en.endereco_id=p.endereco_id where p.cooperativa_id=c.cooperativa_id and p.esta_ativo=true and
en.cidade ilike ?)) order by c.nome
""",
                (r, n) ->
                        new CooperativaResponse(
                                uuid(r, "cooperativa_id"),
                                r.getString("nome"),
                                r.getString("descricao"),
                                r.getInt("numero_cooperados"),
                                r.getString("horario_funcionamento"),
                                null,
                                null,
                                r.getString("cidade")),
                quantidadeMin,
                categoriaId,
                categoriaId,
                cidade,
                cidade == null ? null : "%" + cidade + "%");
    }

    public List<PerfilPublico> buscarPerfilPublico(UUID cooperativaId) {
        return jdbcTemplate.query(
"""
select c.cooperativa_id,c.nome,c.descricao,c.horario_funcionamento,p.email,en.cidade,concat_ws(',
',en.logradouro,en.numero,en.complemento,en.bairro) endereco,(select concat_ws('',t.ddd,t.telefone)
from telefones t where t.perfil_id=p.perfil_id order by t.telefone_id limit 1) telefone from
cooperativas c left join perfis p on p.perfil_id=(select pp.perfil_id from perfis pp where
pp.cooperativa_id=c.cooperativa_id and pp.esta_ativo=true order by pp.data_criacao,pp.perfil_id
limit 1) left join enderecos en on en.endereco_id=p.endereco_id where c.cooperativa_id=?
""",
                (r, n) ->
                        new PerfilPublico(
                                cooperativaId,
                                r.getString("nome"),
                                r.getString("descricao"),
                                r.getString("cidade"),
                                r.getString("endereco"),
                                r.getString("email"),
                                r.getString("telefone"),
                                r.getString("horario_funcionamento"),
                                null,
                                0,
                                List.of()),
                cooperativaId);
    }

    public List<EstoqueResponse> listarEstoquesPublicos(
            UUID cooperativaId, String cooperativaNome) {
        return jdbcTemplate.query(
"""
select e.estoque_id,e.material_id,c.nome_categoria,e.quantidade_kg,e.data_atualizacao from estoques
e join materiais m on m.material_id=e.material_id join categorias_materiais c on
c.categoria_id=m.categoria_id where e.cooperativa_id=? and m.cooperativa_id is null and
m.esta_disponivel=true and e.quantidade_kg>0 order by c.nome_categoria
""",
                (r, n) ->
                        new EstoqueResponse(
                                uuid(r, "estoque_id"),
                                cooperativaId,
                                cooperativaNome,
                                uuid(r, "material_id"),
                                r.getString("nome_categoria"),
                                r.getBigDecimal("quantidade_kg"),
                                data(r, "data_atualizacao")),
                cooperativaId);
    }

    public List<PedidoResponse> listarPedidosPorEmpresa(UUID empresaId) {
        return jdbcTemplate.query(
                PEDIDOS
                        +
"""
 where p.empresa_id=? order by p.data_pedido desc,p.pedido_id
""",
                pedidoMapper,
                empresaId);
    }

    public List<PedidoResponse> buscarPedidoPorEmpresa(UUID pedidoId, UUID empresaId) {
        return jdbcTemplate.query(
                PEDIDOS
                        +
"""
 where p.pedido_id=? and p.empresa_id=?
""",
                pedidoMapper,
                pedidoId,
                empresaId);
    }

    public List<ItemResponse> listarItensPedido(UUID pedidoId) {
        return jdbcTemplate.query(
"""
select i.item_id,i.material_id,c.nome_categoria,i.quantidade_kg,i.preco_unitario from pedido_itens i join
materiais m on m.material_id=i.material_id join categorias_materiais c on
c.categoria_id=m.categoria_id where i.pedido_id=? order by i.item_id
""",
                (r, n) ->
                        new ItemResponse(
                                uuid(r, "item_id"),
                                pedidoId,
                                uuid(r, "material_id"),
                                r.getString("nome_categoria"),
                                r.getBigDecimal("quantidade_kg"),
                                r.getBigDecimal("preco_unitario")),
                pedidoId);
    }

    public List<PedidoCooperativaResponse> listarVinculosPedido(UUID pedidoId) {
        return jdbcTemplate.query(
"""
select pc.pedido_cooperativa_id,pc.cooperativa_id,c.nome,s.status_atual from pedidos_cooperativas pc
join cooperativas c on c.cooperativa_id=pc.cooperativa_id join status s on s.status_id=pc.status_id
where pc.pedido_id=? order by c.nome
""",
                (r, n) ->
                        new PedidoCooperativaResponse(
                                uuid(r, "pedido_cooperativa_id"),
                                pedidoId,
                                uuid(r, "cooperativa_id"),
                                r.getString("nome"),
                                r.getString("status_atual")),
                pedidoId);
    }

    public Long contarPedidosConcluidos(UUID empresaId) {
        return jdbcTemplate.queryForObject(
"""
select count(*) from pedidos p where p.empresa_id=? and exists(
select 1 from pedidos_cooperativas pc join status s on s.status_id=pc.status_id
where pc.pedido_id=p.pedido_id and lower(s.status_atual) in ('finalizado','concluído','concluido'))
""",
                Long.class,
                empresaId);
    }

    public UUID bloquearEmpresa(UUID empresaId) {
        return jdbcTemplate.queryForObject(
"""
select empresa_id from empresas where empresa_id=? for update
""",
                UUID.class,
                empresaId);
    }

    public List<UUID> buscarDonosPedido(UUID pedidoId) {
        return jdbcTemplate.query(
"""
select empresa_id from pedidos where pedido_id=?
""",
                (r, n) -> r.getObject(1, UUID.class),
                pedidoId);
    }

    public Boolean existeCooperativa(UUID cooperativaId) {
        return jdbcTemplate.queryForObject(
"""
select exists(select 1 from cooperativas where cooperativa_id=?)
""",
                Boolean.class,
                cooperativaId);
    }

    public List<UUID> buscarStatusAberto() {
        return jdbcTemplate.query(
"""
select status_id from status where lower(status_atual)='aberto' order by status_id limit 1
""",
                (r, n) -> r.getObject(1, UUID.class));
    }

    public List<BigDecimal> buscarQuantidadesEstoqueComBloqueio(
            UUID cooperativaId, UUID materialId) {
        return jdbcTemplate.query(
"""
select quantidade_kg from estoques where cooperativa_id=? and material_id=? for share
""",
                (r, n) -> r.getBigDecimal(1),
                cooperativaId,
                materialId);
    }

    public int inserirPedido(UUID pedidoId, UUID empresaId) {
        return jdbcTemplate.update(
"""
insert into pedidos(pedido_id,empresa_id) values(?,?)
""",
                pedidoId,
                empresaId);
    }

    public int inserirItem(
            UUID itemId,
            UUID pedidoId,
            UUID materialId,
            BigDecimal quantidadeKg,
            BigDecimal precoUnitario) {
        return jdbcTemplate.update(
"""
insert into pedido_itens(item_id,pedido_id,material_id,quantidade_kg,preco_unitario) values(?,?,?,?,?)
""",
                itemId,
                pedidoId,
                materialId,
                quantidadeKg,
                precoUnitario);
    }

    public int inserirVinculoPedido(
            UUID vinculoId, UUID pedidoId, UUID cooperativaId, UUID statusId) {
        return jdbcTemplate.update(
"""
insert into pedidos_cooperativas(pedido_cooperativa_id,pedido_id,cooperativa_id,status_id)
values(?,?,?,?)
""",
                vinculoId,
                pedidoId,
                cooperativaId,
                statusId);
    }

    public List<AvaliacaoPublica> listarAvaliacoesCooperativa(UUID cooperativaId) {
        return jdbcTemplate.query(
                AV
                        +
"""
 where pv.cooperativa_id=? order by a.data_avaliacao desc
""",
                avaliacaoMapper,
                cooperativaId);
    }

    public List<UUID> buscarPerfisCooperativa(UUID cooperativaId) {
        return jdbcTemplate.query(
"""
select perfil_id from perfis where cooperativa_id=? and esta_ativo=true order by
data_criacao,perfil_id limit 1
""",
                (r, n) -> r.getObject(1, UUID.class),
                cooperativaId);
    }

    public UUID bloquearPedido(UUID pedidoId) {
        return jdbcTemplate.queryForObject(
"""
select pedido_id from pedidos where pedido_id=? for update
""",
                UUID.class,
                pedidoId);
    }

    public Boolean existeAvaliacao(UUID avaliadorId, UUID avaliadoId, UUID pedidoId) {
        return jdbcTemplate.queryForObject(
"""
select exists(select 1 from avaliacoes where avaliador_id=? and avaliado_id=? and pedido_id=?)
""",
                Boolean.class,
                avaliadorId,
                avaliadoId,
                pedidoId);
    }

    public int inserirAvaliacao(
            UUID avaliacaoId,
            UUID avaliadorId,
            UUID avaliadoId,
            UUID pedidoId,
            Integer nota,
            String comentario) {
        return jdbcTemplate.update(
"""
insert into avaliacoes(avaliacao_id,avaliador_id,avaliado_id,pedido_id,nota,comentario)
values(?,?,?,?,?,?)
""",
                avaliacaoId,
                avaliadorId,
                avaliadoId,
                pedidoId,
                nota,
                comentario);
    }

    public List<AvaliacaoPublica> buscarAvaliacao(UUID avaliacaoId) {
        return jdbcTemplate.query(
                AV
                        +
"""
 where a.avaliacao_id=?
""",
                avaliacaoMapper,
                avaliacaoId);
    }

    public Boolean existeCnpj(String cnpj, UUID perfilExcluidoId) {
        return jdbcTemplate.queryForObject(
"""
select exists(select 1 from perfis where regexp_replace(cnpj,'[^0-9]','','g')=? and (cast(? as uuid)
is null or perfil_id<>cast(? as uuid)))
""",
                Boolean.class,
                cnpj,
                perfilExcluidoId,
                perfilExcluidoId);
    }
}
