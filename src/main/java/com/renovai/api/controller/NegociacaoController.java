 
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
 
@RestController
@RequestMapping("/negociacoes")
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
 
    public NegociacaoController(NegociacaoRepository repository,
                                 NegociacaoItemRepository itemRepository,
                                 NegociacaoMensagemRepository mensagemRepository,
                                 PedidoRepository pedidoRepository,
                                 CooperativaRepository cooperativaRepository,
                                 EmpresaRepository empresaRepository,
                                 StatusRepository statusRepository,
                                 MaterialRepository materialRepository,
                                 PerfilRepository perfilRepository) {
        this.repository = repository;
        this.itemRepository = itemRepository;
        this.mensagemRepository = mensagemRepository;
        this.pedidoRepository = pedidoRepository;
        this.cooperativaRepository = cooperativaRepository;
        this.empresaRepository = empresaRepository;
        this.statusRepository = statusRepository;
        this.materialRepository = materialRepository;
        this.perfilRepository = perfilRepository;
    }
 
    @GetMapping("/por-cooperativa/{cooperativaId}")
    @Operation(summary = "Listar negociações da cooperativa — tela 4.5")
    public ResponseEntity<List<NegociacaoResponse>> listarPorCooperativa(@PathVariable UUID cooperativaId) {
        return ResponseEntity.ok(repository.findByCooperativa_CooperativaId(cooperativaId)
                .stream().map(this::toResponse).toList());
    }
 
    @GetMapping("/por-empresa/{empresaId}")
    @Operation(summary = "Listar negociações da empresa — tela 5.4")
    public ResponseEntity<List<NegociacaoResponse>> listarPorEmpresa(@PathVariable UUID empresaId) {
        return ResponseEntity.ok(repository.findByEmpresa_EmpresaId(empresaId)
                .stream().map(this::toResponse).toList());
    }
 
    @GetMapping("/por-pedido/{pedidoId}")
    @Operation(summary = "Listar negociações de um pedido")
    public ResponseEntity<List<NegociacaoResponse>> listarPorPedido(@PathVariable UUID pedidoId) {
        return ResponseEntity.ok(repository.findByPedido_PedidoId(pedidoId)
                .stream().map(this::toResponse).toList());
    }
 
    @GetMapping("/por-cooperativa/{cooperativaId}/status/{statusAtual}")
    @Operation(summary = "Listar negociações da cooperativa por status")
    public ResponseEntity<List<NegociacaoResponse>> listarPorCooperativaEStatus(
            @PathVariable UUID cooperativaId, @PathVariable String statusAtual) {
        return ResponseEntity.ok(repository
                .findByCooperativa_CooperativaIdAndStatus_StatusAtual(cooperativaId, statusAtual)
                .stream().map(this::toResponse).toList());
    }
 
    @GetMapping("/{id}")
    @Operation(summary = "Buscar negociação por ID — tela 4.5.1")
    public ResponseEntity<NegociacaoResponse> buscarPorId(@PathVariable UUID id) {
        Negociacao n = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Negociacao", id));
        return ResponseEntity.ok(toResponse(n));
    }
 
    @PostMapping
    @Operation(summary = "Abrir negociação")
    public ResponseEntity<NegociacaoResponse> criar(@RequestBody @Valid NegociacaoRequest request) {
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
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(repository.save(negociacao)));
    }
 
    @PostMapping("/{id}/contraproposta")
    @Operation(summary = "Enviar contraproposta — tela 4.5.1")
    public ResponseEntity<NegociacaoResponse> contraproposta(
            @PathVariable UUID id,
            @RequestBody @Valid ContrapropostaRequest request) {
        Negociacao n = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Negociacao", id));
        Status emNegociacao = statusRepository
                .findByReferenciaAndStatusAtual("NEGOCIACAO", "EM_NEGOCIACAO")
                .orElseThrow(() -> new RegraDeNegocioException("Status EM_NEGOCIACAO não encontrado."));
        n.setStatus(emNegociacao);
        if (request.valorTotal() != null) n.setValorTotal(request.valorTotal());
        if (request.itens() != null) {
            itemRepository.deleteByNegociacao_NegociacaoId(id);
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
        return ResponseEntity.ok(toResponse(repository.save(n)));
    }
 
    @PatchMapping("/{id}/recusar")
    @Operation(summary = "Recusar negociação — tela 5.4.1")
    public ResponseEntity<NegociacaoResponse> recusar(
            @PathVariable UUID id,
            @RequestBody @Valid RecusarNegociacaoRequest request) {
        Negociacao n = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Negociacao", id));
        Status recusado = statusRepository
                .findByReferenciaAndStatusAtual("NEGOCIACAO", "RECUSADO")
                .orElseThrow(() -> new RegraDeNegocioException("Status RECUSADO não encontrado."));
        n.setStatus(recusado);
        return ResponseEntity.ok(toResponse(repository.save(n)));
    }
 
    @PatchMapping("/{id}/fechar")
    @Operation(summary = "Fechar negociação com valor final — tela 4.5.1")
    public ResponseEntity<NegociacaoResponse> fechar(
            @PathVariable UUID id,
            @RequestBody @Valid FecharNegociacaoRequest request) {
        Negociacao n = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Negociacao", id));
        Status concluido = statusRepository
                .findByReferenciaAndStatusAtual("NEGOCIACAO", "CONCLUIDO")
                .orElseThrow(() -> new RegraDeNegocioException("Status CONCLUIDO não encontrado."));
        n.setStatus(concluido);
        n.setValorTotal(request.valorFinal());
        n.setDataFechamento(java.time.LocalDateTime.now());
        return ResponseEntity.ok(toResponse(repository.save(n)));
    }
 
    @GetMapping("/{id}/mensagens")
    @Operation(summary = "Listar mensagens do chat da negociação — tela 4.5.1")
    public ResponseEntity<List<NegociacaoMensagemResponse>> listarMensagens(@PathVariable UUID id) {
        return ResponseEntity.ok(
                mensagemRepository.findByNegociacao_NegociacaoIdOrderByDataEnvioAsc(id)
                        .stream().map(this::toMensagemResponse).toList()
        );
    }
 
    @PostMapping("/{id}/mensagens")
    @Operation(summary = "Enviar mensagem no chat da negociação — tela 4.5.1")
    public ResponseEntity<NegociacaoMensagemResponse> enviarMensagem(
            @PathVariable UUID id,
            @RequestBody @Valid NegociacaoMensagemRequest request) {
        Negociacao negociacao = repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Negociacao", id));
        Perfil remetente = perfilRepository.findById(request.remetenteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Perfil", request.remetenteId()));
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
            @RequestBody @Valid NegociacaoItemRequest request) {
        Negociacao negociacao = repository.findById(request.negociacaoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Negociacao", request.negociacaoId()));
        Material material = materialRepository.findById(request.materialId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material", request.materialId()));
        NegociacaoItem item = new NegociacaoItem();
        item.setNegociacao(negociacao);
        item.setMaterial(material);
        item.setQuantidadeKg(request.quantidadeKg());
        item.setPrecoUnitario(request.precoUnitario());
        return ResponseEntity.status(HttpStatus.CREATED).body(toItemResponse(itemRepository.save(item)));
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
                itens
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