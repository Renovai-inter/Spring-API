package com.renovai.api.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.renovai.api.model.Cargo;
import com.renovai.api.model.Cooperativa;
import com.renovai.api.model.Funcionario;
import com.renovai.api.repository.FuncionarioRepository;
import com.renovai.api.service.CooperativaService;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class GestorCooperativaControllerTest {

    private FuncionarioRepository funcionarios;
    private CooperativaService cooperativas;
    private GestorCooperativaController controller;
    private final UUID cooperativaId = UUID.randomUUID();
    private final Principal principal = () -> "gestor@renovai.test";

    @BeforeEach
    void configurar() {
        funcionarios = mock(FuncionarioRepository.class);
        cooperativas = mock(CooperativaService.class);
        controller = new GestorCooperativaController(funcionarios, cooperativas);
    }

    @Test
    void gestorConsultaSomenteSuaCooperativa() {
        when(funcionarios.findByUsuario_EmailIgnoreCaseAndEstaAtivoTrue(principal.getName()))
                .thenReturn(List.of(funcionario("GESTOR_COOPERATIVA")));

        var resposta = controller.minhaCooperativa(principal);

        assertEquals(200, resposta.getStatusCode().value());
        verify(cooperativas).buscarPorId(cooperativaId);
    }

    @Test
    void gestorAtualizaSomenteFotoDaPropriaCooperativa() {
        when(funcionarios.findByUsuario_EmailIgnoreCaseAndEstaAtivoTrue(principal.getName()))
                .thenReturn(List.of(funcionario("GESTOR_COOPERATIVA")));
        String url = "https://res.cloudinary.com/renovai/image/upload/foto.jpg";

        var resposta = controller.atualizarFoto(principal,
                new GestorCooperativaController.AtualizarFotoRequest(url));

        assertEquals(200, resposta.getStatusCode().value());
        verify(cooperativas).atualizarImagem(cooperativaId, url);
    }

    @Test
    void gestorNaoPodeEnviarUrlDeOutroServidor() {
        when(funcionarios.findByUsuario_EmailIgnoreCaseAndEstaAtivoTrue(principal.getName()))
                .thenReturn(List.of(funcionario("GESTOR_COOPERATIVA")));

        var resposta = controller.atualizarFoto(principal,
                new GestorCooperativaController.AtualizarFotoRequest("https://example.com/foto.jpg"));

        assertEquals(400, resposta.getStatusCode().value());
        verifyNoInteractions(cooperativas);
    }

    @Test
    void cooperadoNaoPodeUsarRotasDeGestor() {
        when(funcionarios.findByUsuario_EmailIgnoreCaseAndEstaAtivoTrue(principal.getName()))
                .thenReturn(List.of(funcionario("COOPERADO")));

        assertThrows(AccessDeniedException.class, () -> controller.minhaCooperativa(principal));
        verifyNoInteractions(cooperativas);
    }

    private Funcionario funcionario(String cargoNome) {
        Cargo cargo = new Cargo();
        cargo.setCargo(cargoNome);
        Cooperativa cooperativa = new Cooperativa();
        cooperativa.setCooperativaId(cooperativaId);
        Funcionario funcionario = new Funcionario();
        funcionario.setCargo(cargo);
        funcionario.setCooperativa(cooperativa);
        return funcionario;
    }
}
