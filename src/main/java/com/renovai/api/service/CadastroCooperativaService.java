package com.renovai.api.service;

import com.renovai.api.dto.request.CadastroCooperativaRequest;
import com.renovai.api.dto.response.CadastroCooperativaResponse;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.Cooperativa;
import com.renovai.api.model.Endereco;
import com.renovai.api.model.Perfil;
import com.renovai.api.repository.CooperativaRepository;
import com.renovai.api.repository.EnderecoRepository;
import com.renovai.api.repository.PerfilRepository;
import com.renovai.api.repository.UsuarioRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class CadastroCooperativaService {
    private final CooperativaRepository cooperativas;
    private final EnderecoRepository enderecos;
    private final PerfilRepository perfis;
    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;

    public CadastroCooperativaService(
            CooperativaRepository cooperativas,
            EnderecoRepository enderecos,
            PerfilRepository perfis,
            UsuarioRepository usuarios,
            PasswordEncoder passwordEncoder) {
        this.cooperativas = cooperativas;
        this.enderecos = enderecos;
        this.perfis = perfis;
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CadastroCooperativaResponse cadastrar(CadastroCooperativaRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (perfis.existsByEmailIgnoreCase(email) || usuarios.existsByEmailIgnoreCase(email)) {
            throw new RegraDeNegocioException("E-mail já cadastrado.");
        }
        if (perfis.existsByCnpj(request.cnpj())) {
            throw new RegraDeNegocioException("CNPJ já cadastrado.");
        }
        if (request.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new RegraDeNegocioException("Senha deve ter até 72 bytes.");
        }
        String senhaHash = passwordEncoder.encode(request.senha());
        var dadosEndereco = request.endereco();
        Endereco endereco = new Endereco();
        endereco.setCep(dadosEndereco.cep());
        endereco.setLogradouro(dadosEndereco.logradouro().trim());
        endereco.setNumero(dadosEndereco.numero().trim());
        endereco.setComplemento(dadosEndereco.complemento());
        endereco.setBairro(dadosEndereco.bairro().trim());
        endereco.setCidade(dadosEndereco.cidade().trim());
        endereco = enderecos.save(endereco);

        Cooperativa cooperativa = new Cooperativa();
        cooperativa.setNome(request.nome().trim());
        cooperativa.setDescricao(request.descricao());
        cooperativa.setNumeroCooperados(
                request.numeroCooperados() == null ? 0 : request.numeroCooperados());
        cooperativa.setHorarioFuncionamento(request.horarioFuncionamento());
        cooperativa.setImagemUrl(request.imagemUrl());
        cooperativa.setContatoPreferencial(request.contatoPreferencial().trim());
        cooperativa = cooperativas.save(cooperativa);

        // O administrador institucional é Perfil, sem vínculo de empresa ou cargo de funcionário.
        Perfil perfil = new Perfil();
        perfil.setCooperativa(cooperativa);
        perfil.setEndereco(endereco);
        perfil.setEmail(email);
        perfil.setCnpj(request.cnpj());
        perfil.setSenhaHash(senhaHash);
        perfil.setEstaAtivo(true);
        perfil.setDataCriacao(LocalDateTime.now());
        perfil = perfis.saveAndFlush(perfil);
        return new CadastroCooperativaResponse(
                cooperativa.getCooperativaId(),
                perfil.getPerfilId(),
                endereco.getEnderecoId(),
                email,
                "ADMIN_COOPERATIVA");
    }
}
