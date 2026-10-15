package com.renovai.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.renovai.api.controller.CooperativaController;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.*;
import com.renovai.api.repository.*;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CooperativaEnderecoTest {
    private CooperativaRepository cooperativas;
    private PerfilRepository perfis;
    private CooperativaService service;
    private UUID id;

    @BeforeEach
    void preparar() {
        cooperativas = mock(CooperativaRepository.class);
        perfis = mock(PerfilRepository.class);
        service = new CooperativaService(cooperativas, perfis,
                mock(EstoqueRepository.class), mock(AvaliacaoRepository.class));
        id = UUID.randomUUID();
        Cooperativa cooperativa = new Cooperativa();
        cooperativa.setCooperativaId(id);
        cooperativa.setNome("Cooperativa teste");
        when(cooperativas.findById(id)).thenReturn(Optional.of(cooperativa));
    }

    @Test
    void getRetornaEnderecoCompletoDoPerfilAtivo() throws Exception {
        UUID enderecoId = UUID.randomUUID();
        Endereco endereco = Endereco.builder().enderecoId(enderecoId).cep("01001-000")
                .logradouro("Praca da Se").numero("10").complemento("Sala 2")
                .bairro("Se").cidade("Sao Paulo").tipo("Comercial").build();
        when(perfis.findByCooperativa_CooperativaIdAtivo(id))
                .thenReturn(Optional.of(Perfil.builder().endereco(endereco).build()));

        MockMvcBuilders.standaloneSetup(new CooperativaController(service)).build()
                .perform(get("/cooperativas/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cooperativaId").value(id.toString()))
                .andExpect(jsonPath("$.nome").value("Cooperativa teste"))
                .andExpect(jsonPath("$.cidade").value("Sao Paulo"))
                .andExpect(jsonPath("$.endereco.enderecoId").value(enderecoId.toString()))
                .andExpect(jsonPath("$.endereco.cep").value("01001-000"))
                .andExpect(jsonPath("$.endereco.logradouro").value("Praca da Se"))
                .andExpect(jsonPath("$.endereco.numero").value("10"))
                .andExpect(jsonPath("$.endereco.complemento").value("Sala 2"))
                .andExpect(jsonPath("$.endereco.bairro").value("Se"))
                .andExpect(jsonPath("$.endereco.cidade").value("Sao Paulo"))
                .andExpect(jsonPath("$.endereco.tipo").value("Comercial"));
        verify(perfis).findByCooperativa_CooperativaIdAtivo(id);
    }

    @Test
    void retornaEnderecoNuloSemPerfilAtivo() {
        when(perfis.findByCooperativa_CooperativaIdAtivo(id)).thenReturn(Optional.empty());
        var response = service.buscarPorId(id);
        assertThat(response.endereco()).isNull();
        assertThat(response.cidade()).isNull();
    }

    @Test
    void retornaEnderecoNuloQuandoPerfilNaoTemEndereco() {
        when(perfis.findByCooperativa_CooperativaIdAtivo(id))
                .thenReturn(Optional.of(new Perfil()));
        var response = service.buscarPorId(id);
        assertThat(response.endereco()).isNull();
        assertThat(response.cidade()).isNull();
    }

    @Test
    void rejeitaCooperativaInexistente() {
        when(cooperativas.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.buscarPorId(id))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verifyNoInteractions(perfis);
    }
}
