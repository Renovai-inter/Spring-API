package com.renovai.api.controller;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.renovai.api.dto.request.Requests.ContrapropostaRequest;
import com.renovai.api.dto.request.Requests.NegociacaoItemRequest;
import com.renovai.api.dto.request.Requests.NegociacaoMensagemRequest;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.*;
import com.renovai.api.repository.*;
import com.renovai.api.service.NegociacaoFluxoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NegociacaoControllerTest {
    @Mock private NegociacaoRepository repository;
    @Mock private NegociacaoItemRepository itens;
    @Mock private NegociacaoMensagemRepository mensagens;
    @Mock private PedidoRepository pedidos;
    @Mock private CooperativaRepository cooperativas;
    @Mock private EmpresaRepository empresas;
    @Mock private StatusRepository statuses;
    @Mock private MaterialRepository materiais;
    @Mock private PerfilRepository perfis;
    @Mock private NegociacaoFluxoService fluxo;
    @InjectMocks private NegociacaoController controller;
    private Negociacao negociacao;
    private Material material;
    private List<NegociacaoItem> atuais;
    private final Principal principal = () -> "gestor@teste.com";

    @BeforeEach
    void preparar() {
        negociacao = new Negociacao();
        negociacao.setNegociacaoId(UUID.randomUUID());
        negociacao.setPedido(new Pedido());
        negociacao.setEmpresa(new Empresa());
        negociacao.getEmpresa().setEmpresaId(UUID.randomUUID());
        negociacao.setCooperativa(new Cooperativa());
        negociacao.setValorTotal(new BigDecimal("50"));
        material = new Material();
        material.setMaterialId(UUID.randomUUID());
        atuais = new ArrayList<>();
        when(fluxo.bloquear(negociacao.getNegociacaoId())).thenReturn(negociacao);
        when(repository.findById(negociacao.getNegociacaoId())).thenReturn(Optional.of(negociacao));
        when(materiais.findById(material.getMaterialId())).thenReturn(Optional.of(material));
        when(itens.findByNegociacao_NegociacaoId(any())).thenAnswer(i -> List.copyOf(atuais));
        doAnswer(
                        i -> {
                            atuais.clear();
                            return null;
                        })
                .when(itens)
                .deleteByNegociacao_NegociacaoId(any());
        when(itens.save(any()))
                .thenAnswer(
                        i -> {
                            NegociacaoItem item = i.getArgument(0);
                            atuais.add(item);
                            return item;
                        });
    }

    @Test
    void contrapropostaSomenteDeValorRemoveItensDaPropostaAnterior() {
        atuais.add(item());
        var resposta =
                controller
                        .contraproposta(
                                negociacao.getNegociacaoId(),
                                new ContrapropostaRequest(
                                        negociacao.getNegociacaoId(),
                                        new BigDecimal("120"),
                                        null,
                                        null),
                                principal)
                        .getBody();
        assertThat(resposta.valorTotal()).isEqualByComparingTo("120");
        assertThat(resposta.itens()).isEmpty();
        verify(fluxo).registrarObservacao(negociacao, principal.getName(), null, "CONTRAPROPOSTA");
    }

    @Test
    void itensSemTotalCalculamValorDaContraproposta() {
        var resposta =
                controller
                        .contraproposta(negociacao.getNegociacaoId(), proposta(null), principal)
                        .getBody();
        assertThat(resposta.valorTotal()).isEqualByComparingTo("12");
        assertThat(resposta.itens()).hasSize(1);
    }

    @Test
    void totalInformadoPrevaleceSobreSomaDosItens() {
        var resposta =
                controller
                        .contraproposta(
                                negociacao.getNegociacaoId(),
                                proposta(new BigDecimal("120")),
                                principal)
                        .getBody();
        assertThat(resposta.valorTotal()).isEqualByComparingTo("120");
    }

    @Test
    void materiaisRepetidosSaoRejeitadosAntesDaGravacao() {
        var request =
                new ContrapropostaRequest(
                        negociacao.getNegociacaoId(),
                        null,
                        List.of(requestItem(), requestItem()),
                        null);
        assertThatThrownBy(
                        () ->
                                controller.contraproposta(
                                        negociacao.getNegociacaoId(), request, principal))
                .hasMessageContaining("materiais repetidos");
        verify(itens, never()).deleteByNegociacao_NegociacaoId(any());
        verify(itens, never()).save(any());
    }

    @Test
    void inclusaoAvulsaRejeitaMaterialRepetido() {
        atuais.add(item());
        assertThatThrownBy(() -> controller.adicionarItem(requestItem(), principal))
                .hasMessageContaining("materiais repetidos");
        verify(itens, never()).saveAndFlush(any());
    }

    @Test
    void itemDeOutraNegociacaoNaoPodeSerIncluidoNaContraproposta() {
        var item =
                new NegociacaoItemRequest(
                        UUID.randomUUID(),
                        material.getMaterialId(),
                        BigDecimal.ONE,
                        BigDecimal.ONE);
        var request =
                new ContrapropostaRequest(negociacao.getNegociacaoId(), null, List.of(item), null);
        assertThatThrownBy(
                        () ->
                                controller.contraproposta(
                                        negociacao.getNegociacaoId(), request, principal))
                .hasMessageContaining("ID da negociação divergente");
        verify(itens, never()).save(any());
    }

    @Test
    void consultaDevolveObservacaoMaisRecenteELimpezaNaoRecuperaAnterior() {
        var mensagem = new NegociacaoMensagem();
        mensagem.setMensagem("Retirar amanhã.");
        when(mensagens
                        .findFirstByNegociacao_NegociacaoIdAndTipoMensagemOrderByDataEnvioDescMensagemIdDesc(
                                negociacao.getNegociacaoId(), "CONTRAPROPOSTA"))
                .thenReturn(Optional.of(mensagem));
        assertThat(
                        controller
                                .buscarPorId(negociacao.getNegociacaoId(), principal)
                                .getBody()
                                .observacao())
                .isEqualTo("Retirar amanhã.");
        mensagem.setMensagem("");
        assertThat(
                        controller
                                .buscarPorId(negociacao.getNegociacaoId(), principal)
                                .getBody()
                                .observacao())
                .isNull();
    }

    @Test
    void aceiteBloqueiaPropostaEItensMasMantemChat() {
        doThrow(new RegraDeNegocioException("A negociação não está aberta para alteração."))
                .when(fluxo)
                .exigirAberta(negociacao);
        assertThatThrownBy(
                        () ->
                                controller.contraproposta(
                                        negociacao.getNegociacaoId(), proposta(null), principal))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThatThrownBy(() -> controller.adicionarItem(requestItem(), principal))
                .isInstanceOf(RegraDeNegocioException.class);
        Perfil remetente = new Perfil();
        remetente.setPerfilId(UUID.randomUUID());
        when(fluxo.participante(negociacao, principal.getName())).thenReturn(remetente);
        when(mensagens.save(any())).thenAnswer(i -> i.getArgument(0));
        var request =
                new NegociacaoMensagemRequest(
                        negociacao.getNegociacaoId(),
                        remetente.getPerfilId(),
                        "Entrega amanhã.",
                        "TEXTO");
        assertThat(
                        controller
                                .enviarMensagem(negociacao.getNegociacaoId(), request, principal)
                                .getBody()
                                .mensagem())
                .isEqualTo("Entrega amanhã.");
    }

    @Test
    void listagemDaContaUsaEmpresaDoEmailAutenticado() {
        Perfil perfil = new Perfil();
        perfil.setEstaAtivo(true);
        perfil.setEmpresa(negociacao.getEmpresa());
        when(perfis.findByEmailIgnoreCase(principal.getName())).thenReturn(Optional.of(perfil));
        when(repository.findByEmpresa_EmpresaId(negociacao.getEmpresa().getEmpresaId()))
                .thenReturn(List.of(negociacao));
        assertThat(controller.minhasNegociacoes(principal).getBody()).hasSize(1);
        verify(repository).findByEmpresa_EmpresaId(negociacao.getEmpresa().getEmpresaId());
    }

    private ContrapropostaRequest proposta(BigDecimal total) {
        return new ContrapropostaRequest(
                negociacao.getNegociacaoId(), total, List.of(requestItem()), "Entrega amanhã.");
    }

    private NegociacaoItemRequest requestItem() {
        return new NegociacaoItemRequest(
                negociacao.getNegociacaoId(),
                material.getMaterialId(),
                new BigDecimal("6"),
                new BigDecimal("2"));
    }

    private NegociacaoItem item() {
        var item = new NegociacaoItem();
        item.setNegociacao(negociacao);
        item.setMaterial(material);
        item.setQuantidadeKg(new BigDecimal("6"));
        item.setPrecoUnitario(new BigDecimal("2"));
        return item;
    }
}
