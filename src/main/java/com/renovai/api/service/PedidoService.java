package com.renovai.api.service;

import com.renovai.api.dto.request.Requests.*;
import com.renovai.api.dto.response.Responses.*;
import com.renovai.api.exception.RecursoNaoEncontradoException;
import com.renovai.api.model.*;
import com.renovai.api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final EmpresaRepository empresaRepository;
    private final ItemRepository itemRepository;
    private final MaterialRepository materialRepository;
    private final PedidoCooperativaRepository pedidoCooperativaRepository;
    private final CooperativaRepository cooperativaRepository;
    private final StatusRepository statusRepository;

    public PedidoService(PedidoRepository pedidoRepository,
                         EmpresaRepository empresaRepository,
                         ItemRepository itemRepository,
                         MaterialRepository materialRepository,
                         PedidoCooperativaRepository pedidoCooperativaRepository,
                         CooperativaRepository cooperativaRepository,
                         StatusRepository statusRepository) {
        this.pedidoRepository = pedidoRepository;
        this.empresaRepository = empresaRepository;
        this.itemRepository = itemRepository;
        this.materialRepository = materialRepository;
        this.pedidoCooperativaRepository = pedidoCooperativaRepository;
        this.cooperativaRepository = cooperativaRepository;
        this.statusRepository = statusRepository;
    }

    // ── Pedidos ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarTodos() {
        return pedidoRepository.findAll().stream().map(this::toPedidoResponse).toList();
    }

    @Transactional(readOnly = true)
    public PedidoResponse buscarPorId(Integer id) {
        return toPedidoResponse(findPedidoOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listarPorEmpresa(Integer empresaId) {
        return pedidoRepository.findByEmpresa_EmpresaId(empresaId)
                .stream().map(this::toPedidoResponse).toList();
    }

    public PedidoResponse criarPedido(PedidoRequest request) {
        Empresa empresa = empresaRepository.findById(request.empresaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empresa", request.empresaId()));
        Pedido pedido = Pedido.builder().empresa(empresa).build();
        return toPedidoResponse(pedidoRepository.save(pedido));
    }

    public void deletarPedido(Integer id) {
        findPedidoOrThrow(id);
        pedidoRepository.deleteById(id);
    }

    // ── Itens ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ItemResponse> listarItensPorPedido(Integer pedidoId) {
        return itemRepository.findByPedido_PedidoId(pedidoId)
                .stream().map(this::toItemResponse).toList();
    }

    public ItemResponse adicionarItem(ItemRequest request) {
        Pedido pedido = findPedidoOrThrow(request.pedidoId());
        Material material = materialRepository.findById(request.materialId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Material", request.materialId()));

        Item item = Item.builder()
                .pedido(pedido)
                .material(material)
                .quantidadeKg(request.quantidadeKg())
                .precoUnitario(request.precoUnitario())
                .build();
        return toItemResponse(itemRepository.save(item));
    }

    public void removerItem(Integer itemId) {
        itemRepository.findById(itemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item", itemId));
        itemRepository.deleteById(itemId);
    }

    // ── Pedido × Cooperativa ─────────────────────────────────

    @Transactional(readOnly = true)
    public List<PedidoCooperativaResponse> listarPorCooperativa(Integer cooperativaId) {
        return pedidoCooperativaRepository.findByCooperativa_CooperativaId(cooperativaId)
                .stream().map(this::toPedidoCoopResponse).toList();
    }

    public PedidoCooperativaResponse vincularCooperativa(PedidoCooperativaRequest request) {
        Pedido pedido = findPedidoOrThrow(request.pedidoId());
        Cooperativa cooperativa = cooperativaRepository.findById(request.cooperativaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cooperativa", request.cooperativaId()));
        Status status = statusRepository.findById(request.statusId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Status", request.statusId()));

        PedidoCooperativa pc = PedidoCooperativa.builder()
                .pedido(pedido).cooperativa(cooperativa).status(status).build();
        return toPedidoCoopResponse(pedidoCooperativaRepository.save(pc));
    }

    public PedidoCooperativaResponse atualizarStatusPedidoCooperativa(Integer id, Integer novoStatusId) {
        PedidoCooperativa pc = pedidoCooperativaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("PedidoCooperativa", id));
        Status status = statusRepository.findById(novoStatusId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Status", novoStatusId));
        pc.setStatus(status);
        return toPedidoCoopResponse(pedidoCooperativaRepository.save(pc));
    }

    // ── Helpers ──────────────────────────────────────────────

    private Pedido findPedidoOrThrow(Integer id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pedido", id));
    }

    private PedidoResponse toPedidoResponse(Pedido p) {
        return new PedidoResponse(
                p.getPedidoId(),
                p.getEmpresa().getEmpresaId(),
                p.getEmpresa().getNome(),
                p.getDataPedido(),
                p.getDataFinal()
        );
    }

    private ItemResponse toItemResponse(Item i) {
        return new ItemResponse(
                i.getItemId(),
                i.getPedido().getPedidoId(),
                i.getMaterial().getMaterialId(),
                i.getMaterial().getCategoria(),
                i.getQuantidadeKg(),
                i.getPrecoUnitario()
        );
    }

    private PedidoCooperativaResponse toPedidoCoopResponse(PedidoCooperativa pc) {
        return new PedidoCooperativaResponse(
                pc.getPedidoCooperativaId(),
                pc.getPedido().getPedidoId(),
                pc.getCooperativa().getCooperativaId(),
                pc.getCooperativa().getNome(),
                pc.getStatus().getStatusAtual()
        );
    }
}
