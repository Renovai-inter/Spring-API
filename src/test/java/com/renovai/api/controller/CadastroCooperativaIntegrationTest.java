package com.renovai.api.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.renovai.api.model.Perfil;
import com.renovai.api.model.Usuario;
import com.renovai.api.repository.CooperativaRepository;
import com.renovai.api.repository.EnderecoRepository;
import com.renovai.api.repository.PerfilRepository;
import com.renovai.api.repository.UsuarioRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:cadastro-cooperativa;DB_CLOSE_DELAY=-1",
            "spring.datasource.driver-class-name=org.h2.Driver",
            "spring.datasource.username=sa",
            "spring.datasource.password=",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "jwt.secret=chave-exclusiva-de-testes-com-mais-de-32-caracteres"
        })
@AutoConfigureMockMvc
class CadastroCooperativaIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private PasswordEncoder encoder;
    @Autowired private CooperativaRepository cooperativas;
    @Autowired private EnderecoRepository enderecos;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private PerfilRepository perfis;
    @Autowired private JdbcTemplate jdbc;

    private static final String BODY =
            """
            {
              "nome": " Cooperativa Teste ",
              "contatoPreferencial": "(11) 99999-9999",
              "email": "ADMIN@TESTE.COM",
              "cnpj": "12.345.678/0001-90",
              "senha": "senha123",
              "endereco": {
                "cep": "01001-000", "logradouro": "Rua Teste", "numero": "100",
                "bairro": "Centro", "cidade": "São Paulo"
              }
            }
            """;

    @AfterEach
    void limpar() {
        perfis.deleteAll();
        cooperativas.deleteAll();
        enderecos.deleteAll();
        usuarios.deleteAll();
    }

    @Test
    void cadastroPublicoPersisteVinculosEPermiteLoginSomenteComoAdminCooperativa()
            throws Exception {
        // Campos extras não permitem escolher permissões ou vincular registros de terceiros.
        String tentativaDeEscalada =
                BODY.replace(
                        "\"nome\":",
                        "\"role\": \"ADMIN_SITE\",\"empresaId\":"
                                + " \"00000000-0000-0000-0000-000000000001\",\"cooperativaId\":"
                                + " \"00000000-0000-0000-0000-000000000002\",\"nome\":");
        String response =
                mvc.perform(
                                post("/auth/cadastro-cooperativa")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(tentativaDeEscalada))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("email").value("admin@teste.com"))
                        .andExpect(jsonPath("role").value("ADMIN_COOPERATIVA"))
                        .andExpect(jsonPath("senha").doesNotExist())
                        .andExpect(jsonPath("senhaHash").doesNotExist())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        var json = mapper.readTree(response);
        Perfil perfil = perfis.findByEmail("admin@teste.com").orElseThrow();
        assertThat(perfil.getPerfilId().toString()).isEqualTo(json.get("perfilId").asText());
        assertThat(perfil.getCooperativa().getCooperativaId().toString())
                .isEqualTo(json.get("cooperativaId").asText());
        assertThat(perfil.getEndereco().getEnderecoId().toString())
                .isEqualTo(json.get("enderecoId").asText());
        assertThat(perfil.getEmpresa()).isNull();
        assertThat(perfil.getEstaAtivo()).isTrue();
        assertThat(perfil.getDataCriacao()).isNotNull();
        assertThat(perfil.getSenhaHash()).isNotEqualTo("senha123");
        assertThat(encoder.matches("senha123", perfil.getSenhaHash())).isTrue();
        assertThat(cooperativas.findAll().get(0).getNome()).isEqualTo("Cooperativa Teste");
        assertThat(cooperativas.findAll().get(0).getNumeroCooperados()).isZero();
        assertThat(usuarios.count()).isZero();

        String login =
                mvc.perform(
                                post("/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"email\":\"admin@teste.com\",\"senha\":\"senha123\"}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("role").value("ADMIN_COOPERATIVA"))
                        .andExpect(jsonPath("usuarioId").value(perfil.getPerfilId().toString()))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String token = mapper.readTree(login).get("token").asText();
        mvc.perform(
                        get("/cooperativas/" + json.get("cooperativaId").asText())
                                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("cidade").value("São Paulo"));
        mvc.perform(
                        post("/perfis")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"email\":\"outro@teste.com\",\"cnpj\":\"98.765.432/0001-10\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void duplicidadeDeEmailOuCnpjNaoCriaNovosRegistros() throws Exception {
        cadastrar();
        mvc.perform(
                        post("/auth/cadastro-cooperativa")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY.replace("ADMIN@TESTE.COM", "admin@teste.com")))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(
                        post("/auth/cadastro-cooperativa")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY.replace("ADMIN@TESTE.COM", "outro@teste.com")))
                .andExpect(status().isUnprocessableEntity());
        assertThat(cooperativas.count()).isEqualTo(1);
        assertThat(enderecos.count()).isEqualTo(1);
        assertThat(perfis.count()).isEqualTo(1);
    }

    @Test
    void emailDeUsuarioExistenteTambemImpedeCadastro() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNome("Existente");
        usuario.setCpf("123.456.789-00");
        usuario.setEmail("Admin@Teste.Com");
        usuario.setSenhaHash(encoder.encode("senha123"));
        usuarios.saveAndFlush(usuario);
        mvc.perform(
                        post("/auth/cadastro-cooperativa")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY))
                .andExpect(status().isUnprocessableEntity());
        assertVazio();
    }

    @Test
    void validaEnderecoObrigatorioFormatoDocumentosETamanhoDaSenhaEmBytes() throws Exception {
        for (String body :
                new String[] {
                    BODY.replace("01001-000", "01001000"),
                    BODY.replace("12.345.678/0001-90", "12345678000190"),
                    BODY.replace("São Paulo", " "),
                    BODY.replace("senha123", "123"),
                    BODY.replace("\"nome\": \" Cooperativa Teste ", "\"nome\": \"")
                }) {
            mvc.perform(
                            post("/auth/cadastro-cooperativa")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andExpect(status().isBadRequest());
        }
        var semEndereco = mapper.readTree(BODY);
        ((com.fasterxml.jackson.databind.node.ObjectNode) semEndereco).remove("endereco");
        mvc.perform(
                        post("/auth/cadastro-cooperativa")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(semEndereco)))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        post("/auth/cadastro-cooperativa")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY.replace("senha123", "á".repeat(37))))
                .andExpect(status().isUnprocessableEntity());
        assertVazio();
    }

    @Test
    void falhaAoSalvarPerfilDesfazInclusiveEnderecoECooperativaJaInseridos() throws Exception {
        jdbc.execute(
                "alter table perfis add constraint falha_cadastro_teste check (cnpj <>"
                    + " '12.345.678/0001-90')");
        try {
            mvc.perform(
                            post("/auth/cadastro-cooperativa")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(BODY))
                    .andExpect(status().isConflict());
            assertVazio();
        } finally {
            jdbc.execute("alter table perfis drop constraint falha_cadastro_teste");
        }
    }

    @Test
    void endpointsAdministrativosContinuamProtegidosSemToken() throws Exception {
        for (String path : new String[] {"/cooperativas", "/perfis", "/enderecos"}) {
            mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    private void cadastrar() throws Exception {
        mvc.perform(
                        post("/auth/cadastro-cooperativa")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BODY))
                .andExpect(status().isCreated());
    }

    private void assertVazio() {
        assertThat(cooperativas.count()).isZero();
        assertThat(enderecos.count()).isZero();
        assertThat(perfis.count()).isZero();
    }
}
