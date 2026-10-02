package com.renovai.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.renovai.api.dto.request.EmpresaContaRequests.AlterarSenha;
import com.renovai.api.dto.request.EmpresaContaRequests.Atualizar;
import com.renovai.api.dto.request.EmpresaContaRequests.Cadastro;
import com.renovai.api.dto.request.EmpresaContaRequests.EnviarPedido;
import com.renovai.api.dto.request.EmpresaContaRequests.PedidoItem;
import com.renovai.api.dto.request.LoginRequest;
import com.renovai.api.dto.response.EmpresaContaResponses.MeuPerfil;
import com.renovai.api.dto.response.Responses.LoginResponse;
import com.renovai.api.dto.response.Responses.PedidoResponse;
import com.renovai.api.repository.EmpresaSchemaRepository;
import com.renovai.api.repository.EmpresaSchemaRepository.CredencialEmpresa;
import com.renovai.api.security.JwtAuthenticationFilter;
import com.renovai.api.security.JwtTokenProvider;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class EmpresaSchemaServiceTest {
    @Mock private EmpresaSchemaRepository repository;
    private EmpresaSchemaService service;
    private JwtTokenProvider tokenProvider;
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UUID empresaId = UUID.randomUUID();
    private final UUID perfilId = UUID.randomUUID();
    private final UUID usuarioId = UUID.randomUUID();
    private final UUID cooperativaId = UUID.randomUUID();
    private final UUID materialId = UUID.randomUUID();
    private final UUID pedidoId = UUID.randomUUID();
    private static final String EMAIL = "empresa@renovai.com";

    @BeforeEach
    void preparar() {
        tokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(
                tokenProvider, "jwtSecret", "chave-exclusiva-para-testes-renovai-1234567890");
        ReflectionTestUtils.setField(tokenProvider, "jwtExpiration", 60000L);
        service = new EmpresaSchemaService(repository, encoder, tokenProvider);
    }

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void loginDaEmpresaGeraTokenQueOFiltroReconheceComUmaUnicaRole() throws Exception {
        prepararPerfil();
        when(repository.buscarCredenciaisPorEmail(EMAIL))
                .thenReturn(List.of(new CredencialEmpresa(perfilId, encoder.encode("senha123"))));

        LoginResponse response = service.login(new LoginRequest(EMAIL, "senha123"));

        assertThat(response.usuarioId()).isEqualTo(perfilId);
        assertThat(response.tipo()).isEqualTo("Bearer");
        assertThat(response.role()).isEqualTo("GESTOR_EMPRESA");
        assertThat(tokenProvider.extrairRole(response.token())).isEqualTo("GESTOR_EMPRESA");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + response.token());
        new JwtAuthenticationFilter(tokenProvider)
                .doFilter(
                        request,
                        new MockHttpServletResponse(),
                        (req, res) -> {
                            var auth = SecurityContextHolder.getContext().getAuthentication();
                            assertThat(auth.getName()).isEqualTo(EMAIL);
                            assertThat(auth.getAuthorities())
                                    .extracting("authority")
                                    .containsExactly("ROLE_GESTOR_EMPRESA");
                        });
    }

    @Test
    void loginRejeitaSenhaIncorreta() {
        prepararPerfil();
        when(repository.buscarCredenciaisPorEmail(EMAIL))
                .thenReturn(List.of(new CredencialEmpresa(perfilId, encoder.encode("senha123"))));

        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, "outraSenha")))
                .hasMessage("Credenciais inválidas.");
    }

    @Test
    void loginRejeitaCredenciaisAmbiguas() {
        prepararPerfil();
        CredencialEmpresa credencial = new CredencialEmpresa(perfilId, encoder.encode("senha123"));
        when(repository.buscarCredenciaisPorEmail(EMAIL))
                .thenReturn(List.of(credencial, credencial));

        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, "senha123")))
                .hasMessage("Credenciais inválidas.");
    }

    @Test
    void contaInativaNaoPodeEntrar() {
        assertThatThrownBy(() -> service.login(new LoginRequest(EMAIL, "senha123")))
                .hasMessage("Conta da Empresa não encontrada ou inativa.");
        verify(repository, never()).buscarCredenciaisPorEmail(any());
    }

    @Test
    void cadastroNormalizaEmailValidaDocumentosEGravaSomenteHash() {
        var response = service.cadastrar(cadastro(" Empresa@Renovai.com ", "senha123"));

        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.role()).isEqualTo("GESTOR_EMPRESA");
        assertThat(tokenProvider.extrairRole(response.token())).isEqualTo("GESTOR_EMPRESA");
        var hashCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(repository)
                .inserirUsuario(
                        any(),
                        eq("Responsável"),
                        eq(EMAIL),
                        eq("52998224725"),
                        hashCaptor.capture());
        assertThat(hashCaptor.getValue()).isNotEqualTo("senha123");
        assertThat(encoder.matches("senha123", hashCaptor.getValue())).isTrue();
        verify(repository)
                .inserirPerfil(
                        any(),
                        eq(response.empresaId()),
                        any(),
                        eq(EMAIL),
                        eq("11.222.333/0001-81"),
                        eq(hashCaptor.getValue()));
        verify(repository).inserirTelefone(any(), any(), eq("11999999999"));
    }

    @Test
    void cadastroComEmailRepetidoNaoGravaDados() {
        when(repository.contarEmailsCadastrados(EMAIL)).thenReturn(1L);

        assertThatThrownBy(() -> service.cadastrar(cadastro(EMAIL, "senha123")))
                .hasMessage("E-mail já cadastrado.");
        verificarNenhumCadastro();
    }

    @Test
    void cadastroComCpfRepetidoNaoGravaDados() {
        when(repository.existeCpf("52998224725")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(cadastro(EMAIL, "senha123")))
                .hasMessage("CPF já cadastrado.");
        verificarNenhumCadastro();
    }

    @Test
    void cadastroComCnpjRepetidoNaoGravaDados() {
        when(repository.existeCnpj("11222333000181", null)).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(cadastro(EMAIL, "senha123")))
                .hasMessage("CNPJ já cadastrado.");
        verificarNenhumCadastro();
    }

    @Test
    void cadastroRejeitaSenhaQueExcedeLimiteEmBytes() {
        assertThatThrownBy(() -> service.cadastrar(cadastro(EMAIL, "á".repeat(37))))
                .hasMessage("Senha deve ter até 72 bytes.");
        verificarNenhumCadastro();
    }

    @Test
    void alterarSenhaExigeSenhaAtualEGravaHashNovo() {
        prepararPerfil();
        when(repository.buscarHashesPorEmail(EMAIL))
                .thenReturn(List.of(encoder.encode("atual123")));
        when(repository.atualizarSenha(any(), eq(EMAIL))).thenReturn(1);

        service.alterarSenha(EMAIL, new AlterarSenha("atual123", "nova123"));

        var hashCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(repository).atualizarSenha(hashCaptor.capture(), eq(EMAIL));
        assertThat(encoder.matches("nova123", hashCaptor.getValue())).isTrue();
    }

    @Test
    void alterarSenhaRejeitaSenhaAtualIncorreta() {
        prepararPerfil();
        when(repository.buscarHashesPorEmail(EMAIL))
                .thenReturn(List.of(encoder.encode("atual123")));

        assertThatThrownBy(() -> service.alterarSenha(EMAIL, new AlterarSenha("errada", "nova123")))
                .hasMessage("Senha atual incorreta.");
        verify(repository, never()).atualizarSenha(any(), any());
    }

    @Test
    void mudarEmailAtualizaAsCredenciaisEOPerfil() {
        prepararPerfil();
        String novoEmail = "novo@renovai.com";
        MeuPerfil novoPerfil = perfil(novoEmail);
        when(repository.buscarPerfilPorEmail(novoEmail)).thenReturn(List.of(novoPerfil));

        var response =
                service.atualizar(
                        EMAIL,
                        new Atualizar(null, "NOVO@Renovai.com", null, null, null, null, null));

        assertThat(response.email()).isEqualTo(novoEmail);
        verify(repository).atualizarEmailPerfil(novoEmail, perfilId);
    }

    @Test
    void consultaDeItensNaoExponhePedidoDeOutraEmpresa() {
        prepararPerfil();

        assertThatThrownBy(() -> service.itens(EMAIL, pedidoId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        ex ->
                                assertThat(((ResponseStatusException) ex).getStatusCode().value())
                                        .isEqualTo(404));
        verify(repository).buscarPedidoPorEmpresa(pedidoId, empresaId);
        verify(repository, never()).listarItensPedido(any());
    }

    @Test
    void envioValidaSomaDosItensDoMesmoMaterialAntesDeGravar() {
        prepararEnvio();
        when(repository.buscarQuantidadesEstoqueComBloqueio(cooperativaId, materialId))
                .thenReturn(List.of(new BigDecimal("10")));
        EnviarPedido request =
                new EnviarPedido(cooperativaId, pedidoId, List.of(item("6"), item("6")));

        assertThatThrownBy(() -> service.enviar(EMAIL, request))
                .hasMessage("Quantidade solicitada maior que o estoque disponível.");
        verify(repository, never()).inserirPedido(any(), any());
        verify(repository, never()).inserirItem(any(), any(), any(), any(), any());
    }

    @Test
    void envioValidoGravaPedidoItensEVinculoSemBaixarEstoque() {
        prepararEnvio();
        UUID statusId = repository.buscarStatusAberto().get(0);
        when(repository.buscarQuantidadesEstoqueComBloqueio(cooperativaId, materialId))
                .thenReturn(List.of(new BigDecimal("10")));
        PedidoResponse pedido = pedido("Aberto", "12");
        when(repository.buscarPedidoPorEmpresa(pedidoId, empresaId)).thenReturn(List.of(pedido));

        assertThat(
                        service.enviar(
                                EMAIL,
                                new EnviarPedido(cooperativaId, pedidoId, List.of(item("6")))))
                .isEqualTo(pedido);
        verify(repository).inserirPedido(pedidoId, empresaId);
        verify(repository)
                .inserirItem(
                        any(),
                        eq(pedidoId),
                        eq(materialId),
                        eq(new BigDecimal("6")),
                        eq(new BigDecimal("2")));
        verify(repository)
                .inserirVinculoPedido(any(), eq(pedidoId), eq(cooperativaId), eq(statusId));
    }

    @Test
    void reenviarChaveDaMesmaEmpresaRetornaPedidoSemDuplicar() {
        prepararPerfil();
        when(repository.buscarDonosPedido(pedidoId)).thenReturn(List.of(empresaId));
        PedidoResponse pedido = pedido("Aberto", "12");
        when(repository.buscarPedidoPorEmpresa(pedidoId, empresaId)).thenReturn(List.of(pedido));

        assertThat(
                        service.enviar(
                                EMAIL,
                                new EnviarPedido(cooperativaId, pedidoId, List.of(item("6")))))
                .isEqualTo(pedido);
        verify(repository, never()).inserirPedido(any(), any());
        verify(repository, never()).inserirVinculoPedido(any(), any(), any(), any());
    }

    @Test
    void chaveDeOutraEmpresaRetornaConflito() {
        prepararPerfil();
        when(repository.buscarDonosPedido(pedidoId)).thenReturn(List.of(UUID.randomUUID()));

        assertThatThrownBy(
                        () ->
                                service.enviar(
                                        EMAIL,
                                        new EnviarPedido(
                                                cooperativaId, pedidoId, List.of(item("6")))))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        ex ->
                                assertThat(((ResponseStatusException) ex).getStatusCode().value())
                                        .isEqualTo(409));
        verify(repository, never()).inserirPedido(any(), any());
    }

    @Test
    void dashboardSomaSomentePedidosAprovados() {
        prepararPerfil();
        when(repository.listarPedidosPorEmpresa(empresaId))
                .thenReturn(
                        List.of(
                                pedido("Aberto", "100"),
                                pedido("Recusado", "200"),
                                pedido("Aceito", "30"),
                                pedido("Concluído", "40")));
        when(repository.contarPedidosConcluidos(empresaId)).thenReturn(1L);

        var response = service.dashboard(EMAIL);

        assertThat(response.totalPedidosEnviados()).isEqualTo(4);
        assertThat(response.totalPedidosAceitos()).isEqualTo(2);
        assertThat(response.totalPedidosConcluidos()).isEqualTo(1);
        assertThat(response.valorTotalNegociado()).isEqualByComparingTo("70");
    }

    @Test
    void interessesSaoAdicionadosSemSubstituirOsAnteriores() {
        prepararPerfil();
        UUID categoria1 = UUID.randomUUID(), categoria2 = UUID.randomUUID();
        when(repository.buscarCategoria(categoria1)).thenReturn(List.of("Papel"));
        when(repository.buscarCategoria(categoria2)).thenReturn(List.of("Vidro"));
        service.interesse(EMAIL, categoria1);
        service.interesse(EMAIL, categoria2);
        verify(repository).inserirInteresse(empresaId, categoria1);
        verify(repository).inserirInteresse(empresaId, categoria2);
        verify(repository, never()).limparInteresses(any());
    }

    @Test
    void removerInteressesLimpaSomenteAEmpresaAutenticada() {
        prepararPerfil();
        service.interesse(EMAIL, null);
        verify(repository).limparInteresses(empresaId);
        verify(repository, never()).inserirInteresse(any(), any());
    }

    private void prepararPerfil() {
        when(repository.buscarPerfilPorEmail(EMAIL)).thenReturn(List.of(perfil(EMAIL)));
    }

    private MeuPerfil perfil(String email) {
        return new MeuPerfil(
                perfilId,
                empresaId,
                "Empresa",
                null,
                email,
                "11.222.333/0001-81",
                "Rua",
                "11999999999",
                "São Paulo",
                null,
                null,
                null);
    }

    private Cadastro cadastro(String email, String senha) {
        return new Cadastro(
                " Responsável ",
                email,
                "529.982.247-25",
                "11999999999",
                senha,
                " Empresa ",
                "11.222.333/0001-81",
                " Rua ",
                null);
    }

    private void verificarNenhumCadastro() {
        verify(repository, never()).inserirUsuario(any(), any(), any(), any(), any());
        verify(repository, never()).inserirEmpresa(any(), any());
    }

    private void prepararEnvio() {
        prepararPerfil();
        when(repository.existeCooperativa(cooperativaId)).thenReturn(true);
        when(repository.buscarStatusAberto()).thenReturn(List.of(UUID.randomUUID()));
        when(repository.existeMaterialGlobalDisponivel(materialId)).thenReturn(true);
    }

    private PedidoItem item(String peso) {
        return new PedidoItem(materialId, new BigDecimal(peso), new BigDecimal("2"));
    }

    private PedidoResponse pedido(String status, String total) {
        return new PedidoResponse(
                pedidoId,
                empresaId,
                "Empresa",
                LocalDateTime.now(),
                null,
                status,
                new BigDecimal(total),
                null);
    }
}
