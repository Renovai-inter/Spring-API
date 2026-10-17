package com.renovai.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.renovai.api.dto.request.Requests.EstoqueRequest;
import com.renovai.api.model.*;
import com.renovai.api.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class CadastroDatasIniciaisTest {

    @Test
    void criarEstoqueEnviaDataAtualizacaoParaPersistencia() {
        EstoqueRepository estoques = mock(EstoqueRepository.class);
        CooperativaRepository cooperativas = mock(CooperativaRepository.class);
        MaterialRepository materiais = mock(MaterialRepository.class);
        UUID cooperativaId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        when(cooperativas.findById(cooperativaId)).thenReturn(Optional.of(new Cooperativa()));
        when(materiais.findById(materialId)).thenReturn(Optional.of(new Material()));
        LocalDateTime inicio = LocalDateTime.now();
        when(estoques.save(any(Estoque.class))).thenAnswer(invocation -> {
            Estoque estoque = invocation.getArgument(0);
            assertThat(estoque.getDataAtualizacao()).isBetween(inicio, LocalDateTime.now());
            return estoque;
        });

        new EstoqueService(estoques, cooperativas, materiais)
                .criar(new EstoqueRequest(cooperativaId, materialId, BigDecimal.ONE));

        verify(estoques).save(any(Estoque.class));
    }

    @Test
    void criarFuncionarioEnviaDataAdmissaoParaPersistencia() {
        FuncionarioRepository funcionarios = mock(FuncionarioRepository.class);
        UsuarioRepository usuarios = mock(UsuarioRepository.class);
        CargoRepository cargos = mock(CargoRepository.class);
        CooperativaRepository cooperativas = mock(CooperativaRepository.class);
        UUID usuarioId = UUID.randomUUID();
        UUID cargoId = UUID.randomUUID();
        UUID cooperativaId = UUID.randomUUID();
        when(usuarios.findById(usuarioId)).thenReturn(Optional.of(new Usuario()));
        when(cargos.findById(cargoId)).thenReturn(Optional.of(new Cargo()));
        when(cooperativas.findById(cooperativaId)).thenReturn(Optional.of(new Cooperativa()));
        LocalDateTime inicio = LocalDateTime.now();
        when(funcionarios.save(any(Funcionario.class))).thenAnswer(invocation -> {
            Funcionario funcionario = invocation.getArgument(0);
            assertThat(funcionario.getDataAdmissao()).isBetween(inicio, LocalDateTime.now());
            return funcionario;
        });

        new FuncionarioService(funcionarios, usuarios, cargos, cooperativas,
                mock(PasswordEncoder.class))
                .criar(new FuncionarioService.FuncionarioRequest(usuarioId, cargoId, cooperativaId));

        verify(funcionarios).save(any(Funcionario.class));
    }
}
