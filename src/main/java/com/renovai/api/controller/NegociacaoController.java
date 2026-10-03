 
package com.renovai.api.controller;
 
import com.renovai.api.dto.request.Requests.NegociacaoRequest;
import com.renovai.api.dto.request.Requests.NegociacaoItemRequest;
import com.renovai.api.dto.request.Requests.NegociacaoMensagemRequest;
import com.renovai.api.dto.request.Requests.ContrapropostaRequest;
import com.renovai.api.dto.request.Requests.RecusarNegociacaoRequest;
import com.renovai.api.dto.request.Requests.FecharNegociacaoRequest;
import com.renovai.api.dto.response.Responses.NegociacaoResponse;
import com.renovai.api.dto.response.Responses.NegociacaoItemResponse;
import com.renovai.api.dto.response.Responses.NegociacaoMensagemResponse;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.exception.RegraDeNegocioException;
import com.renovai.api.model.*;
import com.renovai.api.repository.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
import java.util.UUID;
import java.security.Principal;
import com.renovai.api.service.NegociacaoFluxoService;
import org.springframework.security.access.prepost.PreAuthorize;
 
@RestController
@RequestMapping({"/negociacoes", "/empresas-conta/negociacoes"})
@Tag(name = "Negociações", description = "Negociações entre empresas e cooperativas — tela 4.5.1 e 5.4.1")
@Transactional
public class NegociacaoController {
 
    private final NegociacaoRepository repository;
    private final NegociacaoItemRepository itemRepository;
    private final NegociacaoMensagemRepository mensagemRepository;
    private final PedidoRepository pedidoRepository;
    private final CooperativaRepository cooperativaRepository;
    private final EmpresaRepository empresaRepository;
    private final StatusRepository statusRepository;
    private final MaterialRepository materialRepository;
    private final PerfilRepository perfilRepository;
    private final NegociacaoFluxoService fluxo;

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<java.util.Map<String,Object>> erro(org.springframework.web.server.ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(java.util.Map.of("status", e.getStatusCode().value(), "mensagem", e.getReason() == null ? "Operação não permitida." : e.getReason()));
    }
 
    public NegociacaoController(NegociacaoRepository repository,
                                 NegociacaoItemRepository itemRepository,
                                 NegociacaoMensagemRepository mensagemRepository,
                                 PedidoRepository pedidoRepository,
                                 CooperativaRepository cooperativaRepository,
                                 EmpresaRepository empresaRepository,
                                 StatusRepository statusRepository,
                                 MaterialRepository materialRepository,
                                 PerfilRepository perfilRepository, NegociacaoFluxoService fluxo) {
        this.repository = repository;
        this.itemRepository = itemRepository;
        this.mensagemRepository = mensagemRepository;
        this.pedidoRepository = pedidoRepository;
        this.cooperativaRepository = cooperativaRepository;
        this.empresaRepository = empresaRepository;
        this.statusRepository = statusRepository;
        this.materialRepository = materialRepository;
        this.perfilRepository = perfilRepository;
        this.fluxo = fluxo;
    }
 
    @GetMapping
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public ResponseEntity<List<NegociacaoResponse>> minhasNegociacoes(Principal principal) {
        Perfil perfil = perfilRepository.findByEmailIgnoreCase(principal.getName()).filter(p -> Boolean.TRUE.equals(p.getEstaAtivo()) && p.getEmpresa() != null)
                .orElseThrow(() -> new RegraDeNegocioException("Conta da Empresa não encontrada."));
        return ResponseEntity.ok(repository.findByEmpresa_EmpresaId(perfil.getEmpresa().getEmpresaId()).stream().map(this::toResponse).toList());
    }

    @GetMapping("/por-cooperativa/{cooperativaId}")
    @Operation(summary = "Listar negociações da cooperativa — tela 4.5")
    public ResponseEntity<List<NegociacaoResponse>> listarPorCooperativa(@PathVariable UUID cooperativaId, Principal principal) {
        return ResponseEntity.ok(repository.findByCooperativa_CooperativaId(cooperativaId)
                .stream().peek(n -> fluxo.participante(n, principal.getName())).map(this::toResponse).toList());
    }
 
    @GetMapping("/por-empresa/{empresaId}")
    @Operation(summary = "Listar negociações da empresa — tela 5.4")
    public ResponseEntity<List<NegociacaoResponse>> listarPorEmpresa(@PathVariable UUID empresaId, Principal principal) {
        return ResponseEntity.ok(repository.findByEmpresa_EmpresaId(empresaId)
                .stream().peek(n -> fluxo.participante(n, principal.getName())).map(this::toResponse).toList());
    }
 
    @GetMapping("/por-pedido/{pedidoId}")
    @Operation(summary = "Listar negociações de um pedido")
    public ResponseEntity<List<NegociacaoResponse>> listarPorPedido(@PathVariable UUID pedidoId, Principal principal) {
        return ResponseEntity.ok(repository.findByPedido_PedidoId(pedidoId)
                .stream().peek(n -> fluxo.participante(n, principal.getName())).map(this::toResponse).toList());
    }
 
    @GetMapping("/por-cooperativa/{cooperativaId}/status/{statusAtual}")
    @Operation(summary = "Listar negociações da cooperativa por status")
    public ResponseEntity<List<NegociacaoResponse>> listarPorCooperativaEStatus(
            @PathVariable UUID cooperativaId, @PathVariable String statusAtual, Principal principal) {
        return ResponseEntity.ok(repository
                .findByCooperativa_CooperativaIdAndStatus_StatusAtual(cooperativaId, statusAtual)
                .stream().peek(n -> fluxo.participante(n, principal.getName())).map(this::toResponse).toList());
    }
 
    @GetMapping("/{id}")
    @Operation(summary = "Buscar negociação por ID — tela 4.5.1")
    public ResponseEntity<NegociacaoResponse> buscarPorId(@PathVariable UUID id, Principal principal) {
        Negociacao n = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Negociacao", id));
        fluxo.participante(n, principal.getName());
        return ResponseEntity.ok(toResponse(n));
    }
 
    @PostMapping
    @Operation(summary = "Abrir negociação")
    public ResponseEntity<NegociacaoResponse> criar(@RequestBody @Valid NegociacaoRequest request, Principal principal) {
        Pedido pedido = pedidoRepository.findById(request.pedidoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", request.pedidoId()));
        Cooperativa cooperativa = cooperativaRepository.findById(request.cooperativaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", request.cooperativaId()));
        Empresa empresa = empresaRepository.findById(request.empresaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa", request.empresaId()));
        Status status = statusRepository.findById(request.statusId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Status", request.statusId()));
        Negociacao negociacao = new Negociacao();
        negociacao.setPedido(pedido);
        negociacao.setCooperativa(cooperativa);
        negociacao.setEmpresa(empresa);
        negociacao.setStatus(status);
        negociacao.setValorTotal(request.valorTotal());
        if (!pedido.getEmpresa().getEmpresaId().equals(empresa.getEmpresaId()))
            throw new RegraDeNegocioException("A Empresa não pertence ao pedido.");
        fluxo.participante(negociacao, principal.getName());
        fluxo.validarAbertura(negociacao);
        if (!status.getStatusId().equals(fluxo.status("NEGOCIACAO", "Em andamento", "EM_NEGOCIACAO").getStatusId()))
            throw new RegraDeNegocioException("Uma negociação deve iniciar em andamento.");
        fluxo.sincronizarVinculo(negociacao, fluxo.status("PEDIDO", "Em negociação", "EM_NEGOCIACAO"));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(repository.save(negociacao)));
    }
 
    @PostMapping("/{id}/contraproposta")
    @PreAuthorize("hasAnyRole('GESTOR_COOPERATIVA','ADMIN_COOPERATIVA')")
    @Operation(summary = "Enviar contraproposta — tela 4.5.1")
    public ResponseEntity<NegociacaoResponse> contraproposta(
            @PathVariable UUID id,
            @RequestBody @Valid ContrapropostaRequest request, Principal principal) {
        Negociacao n = fluxo.bloquear(id);
        fluxo.exigirGestor(n, principal.getName());
        fluxo.exigirAberta(n);
        if (!id.equals(request.negociacaoId())) throw new RegraDeNegocioException("ID da negociação divergente.");
        Status emNegociacao = fluxo.status("NEGOCIACAO", "Em andamento", "EM_NEGOCIACAO");
        n.setStatus(emNegociacao);
        if (request.valorTotal() != null) n.setValorTotal(request.valorTotal());
        if (request.itens() != null) {
            if (request.itens().isEmpty() || request.itens().stream().map(NegociacaoItemRequest::materialId).distinct().count() != request.itens().size())
                throw new RegraDeNegocioException("Informe itens sem materiais repetidos.");
            itemRepository.deleteByNegociacao_NegociacaoId(id);
            itemRepository.flush();
            for (NegociacaoItemRequest ir : request.itens()) {
                Material material = materialRepository.findById(ir.materialId())
                        .orElseThrow(() -> new RecursoNaoEncontradoException("Material", ir.materialId()));
                NegociacaoItem item = new NegociacaoItem();
                item.setNegociacao(n);
                item.setMaterial(material);
                item.setQuantidadeKg(ir.quantidadeKg());
                item.setPrecoUnitario(ir.precoUnitario());
                itemRepository.save(item);
            }
        }
        var itens = itemRepository.findByNegociacao_NegociacaoId(id);
        if (!itens.isEmpty()) {
            java.math.BigDecimal total = itens.stream().map(i -> i.getQuantidadeKg().multiply(i.getPrecoUnitario()).setScale(2, java.math.RoundingMode.HALF_UP)).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            if (request.valorTotal() == null && request.itens() != null) n.setValorTotal(total);
        }
        repository.saveAndFlush(n);
        fluxo.sincronizarVinculo(n, fluxo.status("PEDIDO", "Em negociação", "EM_NEGOCIACAO"));
        fluxo.registrarObservacao(n, principal.getName(), request.observacao(), "CONTRAPROPOSTA");
        return ResponseEntity.ok(toResponse(n));
    }
 
    @PatchMapping("/{id}/aceitar")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    @Operation(summary = "Empresa aceita a contraproposta; conclusão permanece com o gestor")
    public ResponseEntity<NegociacaoResponse> aceitar(@PathVariable UUID id, Principal principal) {
        return ResponseEntity.ok(toResponse(fluxo.aceitar(id, principal.getName())));
    }

    @PatchMapping("/{id}/recusar")
    public ResponseEntity<NegociacaoResponse> recusar(@PathVariable UUID id,
            @RequestBody @Valid RecusarNegociacaoRequest request, Principal principal) {
        return ResponseEntity.ok(toResponse(fluxo.recusar(id, principal.getName(), request.justificativa())));
    }

    @PatchMapping({"/{id}/fechar", "/{id}/concluir"})
    @PreAuthorize("hasAnyRole('GESTOR_COOPERATIVA','ADMIN_COOPERATIVA')")
    @Operation(summary = "Gestor conclui um acordo já aceito pela Empresa")
    public ResponseEntity<NegociacaoResponse> fechar(@PathVariable UUID id,
            @RequestBody @Valid FecharNegociacaoRequest request, Principal principal) {
        return ResponseEntity.ok(toResponse(fluxo.concluir(id, principal.getName(), request.valorFinal(), request.observacao())));
    }

    @GetMapping("/{id}/mensagens")
    @Operation(summary = "Listar mensagens do chat da negociação — tela 4.5.1")
    public ResponseEntity<List<NegociacaoMensagemResponse>> listarMensagens(@PathVariable UUID id, Principal principal) {
        fluxo.participante(repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Negociacao", id)), principal.getName());
        return ResponseEntity.ok(
                mensagemRepository.findByNegociacao_NegociacaoIdOrderByDataEnvioAsc(id)
                        .stream().map(this::toMensagemResponse).toList()
        );
    }
 
    @PostMapping("/{id}/mensagens")
    @Operation(summary = "Enviar mensagem no chat da negociação — tela 4.5.1")
    public ResponseEntity<NegociacaoMensagemResponse> enviarMensagem(
            @PathVariable UUID id,
            @RequestBody @Valid NegociacaoMensagemRequest request, Principal principal) {
        Negociacao negociacao = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Negociacao", id));
        Perfil remetente = fluxo.participante(negociacao, principal.getName());
        if (!id.equals(request.negociacaoId()) || !remetente.getPerfilId().equals(request.remetenteId()))
            throw new RegraDeNegocioException("Remetente ou negociação divergente da conta autenticada.");
        if (request.tipoMensagem() != null && !"TEXTO".equals(request.tipoMensagem()))
            throw new RegraDeNegocioException("Use o endpoint de contraproposta para mensagens de proposta.");
        NegociacaoMensagem mensagem = new NegociacaoMensagem();
        mensagem.setNegociacao(negociacao);
        mensagem.setRemetente(remetente);
        mensagem.setMensagem(request.mensagem());
        mensagem.setTipoMensagem(request.tipoMensagem() != null ? request.tipoMensagem() : "TEXTO");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toMensagemResponse(mensagemRepository.save(mensagem)));
    }
 
    @PostMapping("/itens")
    @Operation(summary = "Adicionar item à negociação")
    public ResponseEntity<NegociacaoItemResponse> adicionarItem(
            @RequestBody @Valid NegociacaoItemRequest request, Principal principal) {
        Negociacao negociacao = fluxo.bloquear(request.negociacaoId());
        fluxo.exigirGestor(negociacao,principal.getName());
        fluxo.exigirAberta(negociacao);
        if (itemRepository.findByNegociacao_NegociacaoId(negociacao.getNegociacaoId()).stream()
                .anyMatch(i -> i.getMaterial().getMaterialId().equals(request.materialId())))
            throw new RegraDeNegocioException("Informe itens sem materiais repetidos.");
        Material material = materialRepository.findById(request.materialId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material", request.materialId()));
        NegociacaoItem item = new NegociacaoItem();
        item.setNegociacao(negociacao);
        item.setMaterial(material);
        item.setQuantidadeKg(request.quantidadeKg());
        item.setPrecoUnitario(request.precoUnitario());
        itemRepository.saveAndFlush(item);
        var total = itemRepository.findByNegociacao_NegociacaoId(negociacao.getNegociacaoId()).stream()
                .map(i -> i.getQuantidadeKg().multiply(i.getPrecoUnitario()).setScale(2, java.math.RoundingMode.HALF_UP))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        negociacao.setValorTotal(total);
        repository.save(negociacao);
        return ResponseEntity.status(HttpStatus.CREATED).body(toItemResponse(item));
    }
 
    private NegociacaoResponse toResponse(Negociacao n) {
        List<NegociacaoItemResponse> itens = itemRepository
                .findByNegociacao_NegociacaoId(n.getNegociacaoId())
                .stream().map(this::toItemResponse).toList();
        return new NegociacaoResponse(
                n.getNegociacaoId(),
                n.getPedido().getPedidoId(),
                n.getCooperativa().getCooperativaId(),
                n.getCooperativa().getNome(),
                n.getEmpresa().getEmpresaId(),
                n.getEmpresa().getNome(),
                n.getStatus() != null ? n.getStatus().getStatusAtual() : null,
                n.getValorTotal(),
                n.getDataInicio(),
                n.getDataFechamento(),
                itens,
                mensagemRepository.findFirstByNegociacao_NegociacaoIdAndTipoMensagemOrderByDataEnvioDescMensagemIdDesc(n.getNegociacaoId(), "CONTRAPROPOSTA")
                        .map(NegociacaoMensagem::getMensagem).filter(m -> !m.isEmpty()).orElse(null)
        );
    }
 
    private NegociacaoItemResponse toItemResponse(NegociacaoItem i) {
        return new NegociacaoItemResponse(
                i.getNegociacaoItemId(),
                i.getNegociacao().getNegociacaoId(),
                i.getMaterial().getMaterialId(),
                i.getMaterial().getCategoria() != null
                        ? i.getMaterial().getCategoria().getNomeCategoria() : null,
                i.getQuantidadeKg(),
                i.getPrecoUnitario()
        );
    }
 
    private NegociacaoMensagemResponse toMensagemResponse(NegociacaoMensagem m) {
        return new NegociacaoMensagemResponse(
                m.getMensagemId(),
                m.getNegociacao().getNegociacaoId(),
                m.getRemetente().getPerfilId(),
                m.getRemetente().getEmail(),
                m.getMensagem(),
                m.getTipoMensagem(),
                m.getDataEnvio()
        );
    }
}
