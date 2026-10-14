package com.renovai.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.renovai.api.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@SpringJUnitConfig(AdminCooperativaPermissionsTest.Config.class)
@WebAppConfiguration
@TestPropertySource(properties = {
        "jwt.secret=chave-exclusiva-de-teste-com-mais-de-32-caracteres",
        "jwt.expiration=60000"
})
class AdminCooperativaPermissionsTest {
    @Autowired WebApplicationContext context;
    @Autowired JwtTokenProvider tokens;
    private MockMvc mvc;

    @BeforeEach
    void preparar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void adminAcumulaSomenteOsPapeisSolicitados() throws Exception {
        String body = mvc.perform(get("/permissoes").header("Authorization", bearer("ADMIN_COOPERATIVA")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(body.split(",")).containsExactlyInAnyOrder(
                "ROLE_ADMIN_COOPERATIVA", "ROLE_GESTOR_COOPERATIVA", "ROLE_COOPERADO", "ROLE_MOTORISTA");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/coletas/teste", "/triagens/teste", "/rateios/teste", "/estoques/teste",
            "/pedidos/teste", "/cooperativas/teste", "/rotas/teste", "/gestor", "/cooperado", "/motorista"})
    void adminAcessaOperacoesEPermissoesDeMetodo(String path) throws Exception {
        mvc.perform(get(path).header("Authorization", bearer("ADMIN_COOPERATIVA")))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN_SITE", "GESTOR_COOPERATIVA", "COOPERADO", "MOTORISTA", "GESTOR_EMPRESA", "FUNCIONARIO_COOPERATIVA"})
    void demaisPerfisMantemSomenteSeuPapel(String role) throws Exception {
        mvc.perform(get("/permissoes").header("Authorization", bearer(role)))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getContentAsString()).isEqualTo("ROLE_" + role));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin/teste", "/empresa"})
    void adminNaoRecebePermissoesDeSiteOuEmpresa(String path) throws Exception {
        mvc.perform(get(path).header("Authorization", bearer("ADMIN_COOPERATIVA")))
                .andExpect(status().isForbidden());
    }

    @Test
    void motoristaContinuaBloqueadoEmColetas() throws Exception {
        mvc.perform(get("/coletas/teste").header("Authorization", bearer("MOTORISTA")))
                .andExpect(status().isForbidden());
    }

    private String bearer(String role) {
        return "Bearer " + tokens.gerarToken("usuario@renovai.com", role);
    }

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfig.class, JwtAuthenticationFilter.class, Endpoints.class})
    static class Config {
        @Bean
        JwtTokenProvider tokens() {
            return new JwtTokenProvider();
        }
    }

    @RestController
    static class Endpoints {
        @GetMapping("/permissoes")
        String permissoes(Authentication auth) {
            return String.join(",", auth.getAuthorities().stream().map(a -> a.getAuthority()).toList());
        }

        @GetMapping({"/coletas/teste", "/triagens/teste", "/rateios/teste", "/estoques/teste",
                "/pedidos/teste", "/cooperativas/teste", "/rotas/teste", "/admin/teste"})
        String operacao() { return "ok"; }

        @GetMapping("/gestor")
        @PreAuthorize("hasRole('GESTOR_COOPERATIVA')")
        public String gestor() { return "ok"; }

        @GetMapping("/cooperado")
        @PreAuthorize("hasRole('COOPERADO')")
        public String cooperado() { return "ok"; }

        @GetMapping("/motorista")
        @PreAuthorize("hasRole('MOTORISTA')")
        public String motorista() { return "ok"; }

        @GetMapping("/empresa")
        @PreAuthorize("hasRole('GESTOR_EMPRESA')")
        public String empresa() { return "ok"; }
    }
}
