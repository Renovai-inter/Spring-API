package com.renovai.api.service;

import com.renovai.api.dto.request.EmpresaContaRequests.AlterarSenha;
import com.renovai.api.dto.request.EmpresaContaRequests.Atualizar;
import com.renovai.api.dto.request.EmpresaContaRequests.Avaliar;
import com.renovai.api.dto.request.EmpresaContaRequests.Cadastro;
import com.renovai.api.dto.request.EmpresaContaRequests.EnviarPedido;
import com.renovai.api.dto.request.EmpresaContaRequests.PedidoItem;
import com.renovai.api.dto.request.LoginRequest;
import com.renovai.api.dto.response.EmpresaContaResponses.AvaliacaoPublica;
import com.renovai.api.dto.response.EmpresaContaResponses.CadastroResponse;
import com.renovai.api.dto.response.EmpresaContaResponses.Dashboard;
import com.renovai.api.dto.response.EmpresaContaResponses.Estrelas;
import com.renovai.api.dto.response.EmpresaContaResponses.Interesse;
import com.renovai.api.dto.response.EmpresaContaResponses.MeuPerfil;
import com.renovai.api.dto.response.EmpresaContaResponses.PerfilPublico;
import com.renovai.api.dto.response.Responses.CooperativaResponse;
import com.renovai.api.dto.response.Responses.EstoqueResponse;
import com.renovai.api.dto.response.Responses.ItemResponse;
import com.renovai.api.dto.response.Responses.LoginResponse;
import com.renovai.api.dto.response.Responses.MaterialResponse;
import com.renovai.api.dto.response.Responses.PedidoCooperativaResponse;
import com.renovai.api.dto.response.Responses.PedidoResponse;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.repository.EmpresaSchemaRepository;
import com.renovai.api.repository.EmpresaSchemaRepository.CredencialEmpresa;
import com.renovai.api.security.JwtTokenProvider;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Regras da conta de empresa, com persistência compatível com o schema fornecido. */
@Service
@Transactional
public class EmpresaSchemaService {
    private static final String ROLE_EMPRESA = "GESTOR_EMPRESA";
    private final EmpresaSchemaRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public EmpresaSchemaService(
            EmpresaSchemaRepository repository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional(readOnly = true)
    public boolean ehEmpresa(String email) {
        return Boolean.TRUE.equals(repository.existeEmpresaPorEmail(email));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        MeuPerfil perfil = meuPerfil(request.email());
        List<CredencialEmpresa> credenciais = repository.buscarCredenciaisPorEmail(perfil.email());
        if (credenciais.size() != 1
                || !passwordEncoder.matches(request.senha(), credenciais.get(0).senhaHash())) {
            throw new RegraDeNegocioException("Credenciais inválidas.");
        }
        return new LoginResponse(
                tokenProvider.gerarToken(perfil.email(), ROLE_EMPRESA),
                perfil.email(),
                ROLE_EMPRESA,
                credenciais.get(0).perfilId());
    }

    public CadastroResponse cadastrar(Cadastro request) {
        String email = normalizarEmail(request.email()),
                cpf = validarDocumento(request.cpf(), 11),
                cnpj = validarDocumento(request.cnpj(), 14);
        Long emails = repository.contarEmailsCadastrados(email);
        if (emails != null && emails > 0) {
            throw new RegraDeNegocioException("E-mail já cadastrado.");
        }
        if (Boolean.TRUE.equals(repository.existeCpf(cpf))) {
            throw new RegraDeNegocioException("CPF já cadastrado.");
        }
        validarCnpj(cnpj, null);
        if (request.senha().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new RegraDeNegocioException("Senha deve ter até 72 bytes.");
        }
        if (request.materialId() != null) {
            validarMaterialGlobal(request.materialId());
        }
        UUID usuarioId = UUID.randomUUID(),
                empresaId = UUID.randomUUID(),
                enderecoId = UUID.randomUUID(),
                perfilId = UUID.randomUUID();
        String senhaHash = passwordEncoder.encode(request.senha());
        repository.inserirUsuario(usuarioId, request.nome().trim(), email, cpf, senhaHash);
        repository.inserirEmpresa(empresaId, request.nomeEmpresa().trim());
        if (request.materialId() != null) {
            repository.inserirInteresse(
                    empresaId, repository.buscarCategoriaMaterial(request.materialId()).get(0));
        }
        repository.inserirEndereco(enderecoId, request.endereco().trim());
        repository.inserirPerfil(
                perfilId, empresaId, enderecoId, email, formatarCnpj(cnpj), senhaHash);
        // chk_telefones_dono é XOR: somente perfil_id é preenchido.
        repository.inserirTelefone(UUID.randomUUID(), perfilId, request.telefone().trim());
        return new CadastroResponse(
                tokenProvider.gerarToken(email, ROLE_EMPRESA), email, ROLE_EMPRESA, empresaId);
    }

    @Transactional(readOnly = true)
    public MeuPerfil meuPerfil(String email) {
        List<MeuPerfil> perfis = repository.buscarPerfilPorEmail(email);
        if (perfis.size() != 1) {
            throw new RegraDeNegocioException("Conta da Empresa não encontrada ou inativa.");
        }
        return perfis.get(0);
    }

    public MeuPerfil atualizar(String email, Atualizar request) {
        MeuPerfil perfil = meuPerfil(email);
        if (request.nomeEmpresa() != null) {
            if (request.nomeEmpresa().isBlank()) {
                throw new RegraDeNegocioException("Nome é obrigatório.");
            }
            repository.atualizarNomeEmpresa(request.nomeEmpresa().trim(), perfil.empresaId());
        }
        if (request.descricao() != null) {
            repository.atualizarDescricaoEmpresa(request.descricao().trim(), perfil.empresaId());
        }
        if (request.cnpj() != null) {
            String cnpj = validarDocumento(request.cnpj(), 14);
            validarCnpj(cnpj, perfil.perfilId());
            repository.atualizarCnpj(formatarCnpj(cnpj), perfil.perfilId());
        }
        if (request.endereco() != null || request.cidade() != null) {
            UUID enderecoId = repository.buscarEnderecoId(perfil.perfilId());
            if (enderecoId == null) {
                enderecoId = UUID.randomUUID();
                repository.inserirEnderecoVazio(enderecoId);
                repository.vincularEndereco(enderecoId, perfil.perfilId());
            }
            if (request.endereco() != null) {
                if (request.endereco().isBlank()) {
                    throw new RegraDeNegocioException("Endereço é obrigatório.");
                }
                repository.atualizarLogradouro(request.endereco().trim(), enderecoId);
            }
            if (request.cidade() != null) {
                repository.atualizarCidade(request.cidade().trim(), enderecoId);
            }
        }
        if (request.telefone() != null) {
            if (request.telefone().isBlank()) {
                throw new RegraDeNegocioException("Telefone é obrigatório.");
            }
            List<UUID> telefoneIds = repository.buscarTelefoneIds(perfil.perfilId());
            if (telefoneIds.isEmpty()) {
                repository.inserirTelefone(
                        UUID.randomUUID(), perfil.perfilId(), request.telefone().trim());
            } else {
                repository.atualizarTelefone(request.telefone().trim(), telefoneIds.get(0));
            }
        }
        String novoEmail = email;
        if (request.email() != null) {
            novoEmail = normalizarEmail(request.email());
            if (novoEmail.isBlank()) {
                throw new RegraDeNegocioException("E-mail é obrigatório.");
            }
            if (!novoEmail.equalsIgnoreCase(email)) {
                if (Boolean.TRUE.equals(repository.existeEmail(novoEmail))) {
                    throw new RegraDeNegocioException("E-mail já cadastrado.");
                }
                repository.atualizarEmailPerfil(novoEmail, perfil.perfilId());
            }
        }
        return meuPerfil(novoEmail);
    }

    public void alterarSenha(String email, AlterarSenha request) {
        MeuPerfil perfil = meuPerfil(email);
        if (request.novaSenha().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new RegraDeNegocioException("Senha deve ter até 72 bytes.");
        }
        List<String> hashes = repository.buscarHashesPorEmail(perfil.email());
        if (hashes.size() != 1 || !passwordEncoder.matches(request.senhaAtual(), hashes.get(0))) {
            throw new RegraDeNegocioException("Senha atual incorreta.");
        }
        int atualizados =
                repository.atualizarSenha(
                        passwordEncoder.encode(request.novaSenha()), perfil.email());
        if (atualizados != 1) {
            throw new RegraDeNegocioException("Não foi possível alterar a senha da Empresa.");
        }
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> materiais() {
        return repository.listarMateriaisGlobais();
    }

    private void validarMaterialGlobal(UUID materialId) {
        if (!Boolean.TRUE.equals(repository.existeMaterialGlobalDisponivel(materialId))) {
            throw new RegraDeNegocioException("Selecione um material global disponível.");
        }
    }

    public Interesse interesse(String email, UUID categoriaId) {
        MeuPerfil perfil = meuPerfil(email);
        repository.bloquearEmpresa(perfil.empresaId());
        if (categoriaId == null) {
            repository.limparInteresses(perfil.empresaId());
            return new Interesse(perfil.empresaId(), null, null);
        }
        List<String> categorias = repository.buscarCategoria(categoriaId);
        if (categorias.isEmpty()) {
            throw new RegraDeNegocioException("Categoria não encontrada.");
        }
        repository.inserirInteresse(perfil.empresaId(), categoriaId);
        return new Interesse(perfil.empresaId(), categoriaId, categorias.get(0));
    }

    @Transactional(readOnly = true)
    public List<Interesse> interesses(String email) {
        return repository.listarInteresses(meuPerfil(email).empresaId());
    }

    public List<Interesse> substituirInteresses(String email, List<UUID> categoriaIds) {
        UUID empresaId = meuPerfil(email).empresaId();
        repository.bloquearEmpresa(empresaId);
        List<UUID> categorias = categoriaIds.stream().distinct().toList();
        for (UUID categoriaId : categorias) {
            if (repository.buscarCategoria(categoriaId).isEmpty()) {
                throw new RegraDeNegocioException("Categoria não encontrada.");
            }
        }
        repository.limparInteresses(empresaId);
        categorias.forEach(categoriaId -> repository.inserirInteresse(empresaId, categoriaId));
        return repository.listarInteresses(empresaId);
    }

    public void removerInteresse(String email, UUID categoriaId) {
        UUID empresaId = meuPerfil(email).empresaId();
        repository.bloquearEmpresa(empresaId);
        repository.removerInteresse(empresaId, categoriaId);
    }

    @Transactional(readOnly = true)
    public List<com.renovai.api.dto.response.Responses.FavoritoResponse> favoritos(String email) {
        return repository.listarFavoritos(meuPerfil(email).empresaId());
    }

    public com.renovai.api.dto.response.Responses.FavoritoResponse favoritar(String email, UUID cooperativaId) {
        UUID empresaId = meuPerfil(email).empresaId();
        repository.bloquearEmpresa(empresaId);
        if (!Boolean.TRUE.equals(repository.existeCooperativa(cooperativaId))) {
            throw new RegraDeNegocioException("Cooperativa não encontrada.");
        }
        repository.adicionarFavorito(empresaId, cooperativaId);
        return repository.listarFavoritos(empresaId).stream().filter(f -> cooperativaId.equals(f.cooperativaId())).findFirst().orElseThrow();
    }

    public void desfavoritar(String email, UUID cooperativaId) {
        UUID empresaId = meuPerfil(email).empresaId();
        repository.bloquearEmpresa(empresaId);
        repository.removerFavorito(empresaId, cooperativaId);
    }

    @Transactional(readOnly = true)
    public List<CooperativaResponse> buscar(
            UUID categoriaId, String cidade, BigDecimal quantidadeMin) {
        if (quantidadeMin != null && quantidadeMin.signum() < 0) {
            throw new RegraDeNegocioException("Quantidade mínima inválida.");
        }
        return repository.buscarCooperativas(
                quantidadeMin == null ? BigDecimal.ZERO : quantidadeMin, categoriaId, cidade);
    }

    @Transactional(readOnly = true)
    public PerfilPublico publico(UUID cooperativaId) {
        List<PerfilPublico> perfis = repository.buscarPerfilPublico(cooperativaId);
        if (perfis.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dados não encontrados.");
        }
        PerfilPublico perfil = perfis.get(0);
        List<AvaliacaoPublica> avaliacoes = avaliacoes(cooperativaId);
        Double media =
                avaliacoes.isEmpty()
                        ? null
                        : avaliacoes.stream()
                                .filter(a -> a.nota() != null)
                                .mapToInt(AvaliacaoPublica::nota)
                                .average()
                                .orElse(0);
        List<EstoqueResponse> estoque =
                repository.listarEstoquesPublicos(cooperativaId, perfil.nome());
        return new PerfilPublico(
                cooperativaId,
                perfil.nome(),
                perfil.descricao(),
                perfil.cidade(),
                perfil.endereco(),
                perfil.email(),
                perfil.telefone(),
                perfil.horarioFuncionamento(),
                media,
                avaliacoes.size(),
                estoque);
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> pedidos(String email) {
        MeuPerfil perfil = meuPerfil(email);
        return repository.listarPedidosPorEmpresa(perfil.empresaId());
    }

    @Transactional(readOnly = true)
    public PedidoResponse pedido(String email, UUID pedidoId) {
        MeuPerfil perfil = meuPerfil(email);
        List<PedidoResponse> pedidos =
                repository.buscarPedidoPorEmpresa(pedidoId, perfil.empresaId());
        if (pedidos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Dados não encontrados.");
        }
        return pedidos.get(0);
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> itens(String email, UUID pedidoId) {
        pedido(email, pedidoId);
        return repository.listarItensPedido(pedidoId);
    }

    @Transactional(readOnly = true)
    public List<PedidoCooperativaResponse> vinculos(String email, UUID pedidoId) {
        pedido(email, pedidoId);
        return repository.listarVinculosPedido(pedidoId);
    }

    @Transactional(readOnly = true)
    public Dashboard dashboard(String email) {
        List<PedidoResponse> pedidos = pedidos(email);
        long aceitos = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (PedidoResponse pedido : pedidos) {
            if (statusAprovado(pedido.statusAtual())) {
                aceitos++;
                total = total.add(pedido.valorTotal());
            }
        }
        Long concluidos = repository.contarPedidosConcluidos(meuPerfil(email).empresaId());
        return new Dashboard(pedidos.size(), aceitos, concluidos == null ? 0 : concluidos, total);
    }

    public PedidoResponse enviar(String email, EnviarPedido request) {
        MeuPerfil perfil = meuPerfil(email);
        // O UUID da solicitação é a PK do pedido existente: idempotência sem nova coluna.
        repository.bloquearEmpresa(perfil.empresaId());
        List<UUID> donos = repository.buscarDonosPedido(request.chaveSolicitacao());
        if (!donos.isEmpty()) {
            if (!perfil.empresaId().equals(donos.get(0))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Solicitação inválida.");
            }
            return pedido(email, request.chaveSolicitacao());
        }
        if (!Boolean.TRUE.equals(repository.existeCooperativa(request.cooperativaId()))) {
            throw new RegraDeNegocioException("Cooperativa não encontrada.");
        }
        List<UUID> statusAbertos = repository.buscarStatusAberto();
        if (statusAbertos.isEmpty()) {
            throw new RegraDeNegocioException(
                    "O status Aberto não está cadastrado. Nenhum pedido foi gravado.");
        }
        Map<UUID, BigDecimal> pesos = new HashMap<>();
        for (PedidoItem item : request.itens()) {
            pesos.merge(item.materialId(), item.quantidadeKg(), BigDecimal::add);
        }
        for (var entrada : pesos.entrySet()) {
            validarMaterialGlobal(entrada.getKey());
            List<BigDecimal> quantidades =
                    repository.buscarQuantidadesEstoqueComBloqueio(
                            request.cooperativaId(), entrada.getKey());
            if (quantidades.isEmpty() || quantidades.get(0).compareTo(entrada.getValue()) < 0) {
                throw new RegraDeNegocioException(
                        "Quantidade solicitada maior que o estoque disponível.");
            }
        }
        repository.inserirPedido(request.chaveSolicitacao(), perfil.empresaId());
        for (PedidoItem item : request.itens()) {
            repository.inserirItem(
                    UUID.randomUUID(),
                    request.chaveSolicitacao(),
                    item.materialId(),
                    item.quantidadeKg(),
                    item.precoUnitario());
        }
        repository.inserirVinculoPedido(
                UUID.randomUUID(),
                request.chaveSolicitacao(),
                request.cooperativaId(),
                statusAbertos.get(0));
        // Envio não é venda: saldo só pode ser baixado pelo fluxo de aprovação da cooperativa.
        return pedido(email, request.chaveSolicitacao());
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoPublica> avaliacoes(UUID cooperativaId) {
        return repository.listarAvaliacoesCooperativa(cooperativaId);
    }

    @Transactional(readOnly = true)
    public Estrelas estrelas(UUID cooperativaId) {
        long[] quantidades = new long[5];
        for (AvaliacaoPublica avaliacao : avaliacoes(cooperativaId)) {
            if (avaliacao.nota() != null && avaliacao.nota() >= 1 && avaliacao.nota() <= 5) {
                quantidades[avaliacao.nota() - 1]++;
            }
        }
        return new Estrelas(
                quantidades[0], quantidades[1], quantidades[2], quantidades[3], quantidades[4]);
    }

    public AvaliacaoPublica avaliar(String email, Avaliar request) {
        MeuPerfil perfil = meuPerfil(email);
        pedido(email, request.pedidoId());
        boolean permitida =
                vinculos(email, request.pedidoId()).stream()
                        .anyMatch(
                                v ->
                                        v.cooperativaId().equals(request.cooperativaId())
                                                && statusAprovado(v.statusAtual()));
        if (!permitida) {
            throw new RegraDeNegocioException(
                    "Avalie apenas cooperativas de pedidos aceitos ou concluídos.");
        }
        List<UUID> perfis = repository.buscarPerfisCooperativa(request.cooperativaId());
        if (perfis.isEmpty()) {
            throw new RegraDeNegocioException("Perfil da cooperativa não encontrado.");
        }
        repository.bloquearPedido(request.pedidoId());
        if (Boolean.TRUE.equals(
                repository.existeAvaliacao(perfil.perfilId(), perfis.get(0), request.pedidoId()))) {
            throw new RegraDeNegocioException("Você já avaliou esta cooperativa neste pedido.");
        }
        UUID avaliacaoId = UUID.randomUUID();
        repository.inserirAvaliacao(
                avaliacaoId,
                perfil.perfilId(),
                perfis.get(0),
                request.pedidoId(),
                request.nota(),
                request.comentario());
        return repository.buscarAvaliacao(avaliacaoId).get(0);
    }

    private void validarCnpj(String cnpj, UUID perfilExcluidoId) {
        Boolean existe = repository.existeCnpj(cnpj, perfilExcluidoId);
        if (Boolean.TRUE.equals(existe)) {
            throw new RegraDeNegocioException("CNPJ já cadastrado.");
        }
    }

    private static String validarDocumento(String valor, int tamanho) {
        String documento = valor == null ? "" : valor.replaceAll("[^0-9]", "");
        if (documento.length() != tamanho || documento.chars().distinct().count() == 1) {
            throw new RegraDeNegocioException("Documento inválido.");
        }
        int[] pesos =
                tamanho == 11
                        ? new int[] {10, 9, 8, 7, 6, 5, 4, 3, 2}
                        : new int[] {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        for (int etapa = 0; etapa < 2; etapa++) {
            int soma = 0;
            for (int i = 0; i < pesos.length; i++) {
                soma += (documento.charAt(i) - '0') * pesos[i];
            }
            int resto = soma % 11, digito = resto < 2 ? 0 : 11 - resto;
            if (documento.charAt(pesos.length) - '0' != digito) {
                throw new RegraDeNegocioException("Documento inválido.");
            }
            int[] proximosPesos = new int[pesos.length + 1];
            if (tamanho == 11) {
                for (int i = 0; i < proximosPesos.length; i++) {
                    proximosPesos[i] = 11 - i;
                }
            } else {
                proximosPesos[0] = 6;
                System.arraycopy(pesos, 0, proximosPesos, 1, pesos.length);
            }
            pesos = proximosPesos;
        }
        return documento;
    }

    private static String formatarCnpj(String documento) {
        return documento.substring(0, 2)
                + "."
                + documento.substring(2, 5)
                + "."
                + documento.substring(5, 8)
                + "/"
                + documento.substring(8, 12)
                + "-"
                + documento.substring(12);
    }

    private static String normalizarStatus(String status) {
        return status == null
                ? ""
                : Normalizer.normalize(status, Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "")
                        .toUpperCase(Locale.ROOT);
    }

    private static boolean statusAprovado(String status) {
        return Set.of("ACEITO", "FINALIZADO", "CONCLUIDO").contains(normalizarStatus(status));
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
