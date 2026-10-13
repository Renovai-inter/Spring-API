package com.renovai.api.service;

import com.renovai.api.dto.request.CadastroCooperativaRequest;
import com.renovai.api.dto.response.CadastroCooperativaResponse;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.Cargo;
import com.renovai.api.model.Cooperativa;
import com.renovai.api.model.Endereco;
import com.renovai.api.model.Funcionario;
import com.renovai.api.model.Perfil;
import com.renovai.api.model.Usuario;
import com.renovai.api.repository.CargoRepository;
import com.renovai.api.repository.CooperativaRepository;
import com.renovai.api.repository.EnderecoRepository;
import com.renovai.api.repository.FuncionarioRepository;
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
    private final CargoRepository cargos;
    private final FuncionarioRepository funcionarios;

    public CadastroCooperativaService(
            CooperativaRepository cooperativas,
            EnderecoRepository enderecos,
            PerfilRepository perfis,
            UsuarioRepository usuarios,
            PasswordEncoder passwordEncoder,
            CargoRepository cargos,
            FuncionarioRepository funcionarios) {
        this.cooperativas = cooperativas;
        this.enderecos = enderecos;
        this.perfis = perfis;
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.cargos = cargos;
        this.funcionarios = funcionarios;
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
        if (usuarios.existsByCpf(request.cpfResponsavel())) {
            throw new RegraDeNegocioException("CPF já cadastrado.");
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

        // O perfil representa a instituição; a credencial pessoal fica somente em Usuario.
        Perfil perfil = new Perfil();
        perfil.setCooperativa(cooperativa);
        perfil.setEndereco(endereco);
        perfil.setEmail(email);
        perfil.setCnpj(request.cnpj());
        perfil.setEstaAtivo(true);
        perfil.setDataCriacao(LocalDateTime.now());
        perfil = perfis.saveAndFlush(perfil);
        var usuario = new Usuario();
        usuario.setNome(request.nomeResponsavel().trim());
        usuario.setCpf(request.cpfResponsavel());
        usuario.setEmail(email);
        usuario.setSenhaHash(senhaHash);
        usuario = usuarios.saveAndFlush(usuario);
        var cargo = cargos.findByCargo("Admin").orElseGet(() -> {
            var novo = new Cargo();
            novo.setCargo("Admin");
            return cargos.saveAndFlush(novo);
        });
        var funcionario = new Funcionario();
        funcionario.setUsuario(usuario);
        funcionario.setCooperativa(cooperativa);
        funcionario.setCargo(cargo);
        funcionario = funcionarios.saveAndFlush(funcionario);
        return new CadastroCooperativaResponse(
                cooperativa.getCooperativaId(),
                perfil.getPerfilId(),
                endereco.getEnderecoId(),
                email,
                "ADMIN_COOPERATIVA", usuario.getUsuarioId(), funcionario.getFuncionarioId());
    }
}
