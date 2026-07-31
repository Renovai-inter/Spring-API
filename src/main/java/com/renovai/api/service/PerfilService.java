package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.PerfilRequest;
import com.renovai.api.dto.response.Responses.PerfilResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.Cooperativa;
import com.renovai.api.model.Empresa;
import com.renovai.api.model.Endereco;
import com.renovai.api.model.Perfil;
import com.renovai.api.repository.CooperativaRepository;
import com.renovai.api.repository.EmpresaRepository;
import com.renovai.api.repository.EnderecoRepository;
import com.renovai.api.repository.PerfilRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PerfilService {

    private final PerfilRepository perfilRepository;
    private final EmpresaRepository empresaRepository;
    private final CooperativaRepository cooperativaRepository;
    private final EnderecoRepository enderecoRepository;

    public PerfilService(PerfilRepository perfilRepository,
                         EmpresaRepository empresaRepository,
                         CooperativaRepository cooperativaRepository,
                         EnderecoRepository enderecoRepository) {
        this.perfilRepository = perfilRepository;
        this.empresaRepository = empresaRepository;
        this.cooperativaRepository = cooperativaRepository;
        this.enderecoRepository = enderecoRepository;
    }

    @Transactional(readOnly = true)
    public List<PerfilResponse> listarTodos() {
        return perfilRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PerfilResponse> listarAtivos() {
        return perfilRepository.findByEstaAtivoTrue().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PerfilResponse buscarPorId(UUID id) {
        return toResponse(findOrThrow(id));
    }

    public PerfilResponse criar(PerfilRequest request) {
        Empresa empresa = request.empresaId() != null
                ? empresaRepository.findById(request.empresaId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa", request.empresaId()))
                : null;

        Cooperativa cooperativa = request.cooperativaId() != null
                ? cooperativaRepository.findById(request.cooperativaId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", request.cooperativaId()))
                : null;

        Endereco endereco = request.enderecoId() != null
                ? enderecoRepository.findById(request.enderecoId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Endereco", request.enderecoId()))
                : null;

        Perfil perfil = Perfil.builder()
                .empresa(empresa)
                .cooperativa(cooperativa)
                .endereco(endereco)
                .email(request.email())
                .cnpj(request.cnpj())
                .build();

        return toResponse(perfilRepository.save(perfil));
    }

    public PerfilResponse atualizar(UUID id, PerfilRequest request) {
        Perfil perfil = findOrThrow(id);

        Empresa empresa = request.empresaId() != null
                ? empresaRepository.findById(request.empresaId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa", request.empresaId()))
                : null;

        Cooperativa cooperativa = request.cooperativaId() != null
                ? cooperativaRepository.findById(request.cooperativaId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", request.cooperativaId()))
                : null;

        Endereco endereco = request.enderecoId() != null
                ? enderecoRepository.findById(request.enderecoId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Endereco", request.enderecoId()))
                : null;

        perfil.setEmpresa(empresa);
        perfil.setCooperativa(cooperativa);
        perfil.setEndereco(endereco);
        perfil.setEmail(request.email());
        perfil.setCnpj(request.cnpj());

        return toResponse(perfilRepository.save(perfil));
    }

    public PerfilResponse desativar(UUID id) {
        Perfil perfil = findOrThrow(id);
        perfil.setEstaAtivo(false);
        return toResponse(perfilRepository.save(perfil));
    }

    public void deletar(UUID id) {
        findOrThrow(id);
        perfilRepository.deleteById(id);
    }

    private Perfil findOrThrow(UUID id) {
        return perfilRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil", id));
    }

    private PerfilResponse toResponse(Perfil p) {
        return new PerfilResponse(
                p.getPerfilId(),
                p.getEmail(),
                p.getCnpj(),
                p.getEstaAtivo(),
                p.getDataCriacao(),
                p.getEmpresa() != null ? p.getEmpresa().getEmpresaId() : null,
                p.getEmpresa() != null ? p.getEmpresa().getNome() : null,
                p.getCooperativa() != null ? p.getCooperativa().getCooperativaId() : null,
                p.getCooperativa() != null ? p.getCooperativa().getNome() : null
        );
    }
}