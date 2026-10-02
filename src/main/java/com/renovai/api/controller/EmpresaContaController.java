package com.renovai.api.controller;

import com.renovai.api.dto.request.EmpresaContaRequests.AlterarSenha;
import com.renovai.api.dto.request.EmpresaContaRequests.Atualizar;
import com.renovai.api.dto.request.EmpresaContaRequests.Avaliar;
import com.renovai.api.dto.request.EmpresaContaRequests.Cadastro;
import com.renovai.api.dto.request.EmpresaContaRequests.EnviarPedido;
import com.renovai.api.dto.request.EmpresaContaRequests.InteresseRequest;
import com.renovai.api.dto.response.EmpresaContaResponses.AvaliacaoPublica;
import com.renovai.api.dto.response.EmpresaContaResponses.CadastroResponse;
import com.renovai.api.dto.response.EmpresaContaResponses.Dashboard;
import com.renovai.api.dto.response.EmpresaContaResponses.Estrelas;
import com.renovai.api.dto.response.EmpresaContaResponses.Interesse;
import com.renovai.api.dto.response.EmpresaContaResponses.MeuPerfil;
import com.renovai.api.dto.response.EmpresaContaResponses.PerfilPublico;
import com.renovai.api.dto.response.Responses.CooperativaResponse;
import com.renovai.api.dto.response.Responses.ItemResponse;
import com.renovai.api.dto.response.Responses.MaterialResponse;
import com.renovai.api.dto.response.Responses.PedidoCooperativaResponse;
import com.renovai.api.dto.response.Responses.PedidoResponse;
import com.renovai.api.service.EmpresaSchemaService;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.UUID;

/** Contratos da Empresa que consultam somente o schema PostgreSQL anexado. */
@RestController
public class EmpresaContaController {
    private final EmpresaSchemaService service;

    public EmpresaContaController(EmpresaSchemaService service) {
        this.service = service;
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<java.util.Map<String, Object>> erro(
            org.springframework.web.server.ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(
                        java.util.Map.of(
                                "status",
                                e.getStatusCode().value(),
                                "mensagem",
                                e.getReason() == null
                                        ? "Não foi possível concluir a operação."
                                        : e.getReason()));
    }

    @PostMapping("/auth/cadastro")
    public ResponseEntity<CadastroResponse> cadastrar(@RequestBody @Valid Cadastro body) {
        return ResponseEntity.status(201).body(service.cadastrar(body));
    }

    @GetMapping("/auth/cadastro/materiais")
    public List<MaterialResponse> materiaisCadastro() {
        return service.materiais();
    }

    @GetMapping("/empresas-conta/meu-perfil")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public MeuPerfil meuPerfil(Principal p) {
        return service.meuPerfil(p.getName());
    }

    @PatchMapping("/empresas-conta/meu-perfil")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public MeuPerfil atualizar(Principal p, @RequestBody @Valid Atualizar body) {
        return service.atualizar(p.getName(), body);
    }

    @PatchMapping("/empresas-conta/senha")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public ResponseEntity<Void> senha(Principal p, @RequestBody @Valid AlterarSenha body) {
        service.alterarSenha(p.getName(), body);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/empresas-conta/dashboard")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public Dashboard dashboard(Principal p) {
        return service.dashboard(p.getName());
    }

    @GetMapping("/empresas-conta/interesse")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public List<Interesse> interesse(Principal p) {
        return service.interesses(p.getName());
    }

    @PostMapping("/empresas-conta/interesse")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public Interesse interesse(Principal p, @RequestBody @Valid InteresseRequest b) {
        return service.interesse(p.getName(), b.categoriaId());
    }

    @DeleteMapping("/empresas-conta/interesse")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public ResponseEntity<Void> limparInteresse(Principal p) {
        service.interesse(p.getName(), null);
        return ResponseEntity.noContent().build();
    }

    @org.springframework.web.bind.annotation.PutMapping("/empresas-conta/interesse")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public List<Interesse> substituirInteresses(Principal p, @RequestBody @Valid com.renovai.api.dto.request.EmpresaContaRequests.SubstituirInteresses body) {
        return service.substituirInteresses(p.getName(), body.categoriaIds());
    }

    @DeleteMapping("/empresas-conta/interesse/{categoriaId}")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public ResponseEntity<Void> removerInteresse(Principal p, @PathVariable UUID categoriaId) {
        service.removerInteresse(p.getName(), categoriaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/empresas-conta/favoritos")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public List<com.renovai.api.dto.response.Responses.FavoritoResponse> favoritos(Principal p) {
        return service.favoritos(p.getName());
    }

    @PostMapping("/empresas-conta/favoritos")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public com.renovai.api.dto.response.Responses.FavoritoResponse favoritar(Principal p, @RequestBody @Valid com.renovai.api.dto.request.EmpresaContaRequests.FavoritoContaRequest body) {
        return service.favoritar(p.getName(), body.cooperativaId());
    }

    @DeleteMapping("/empresas-conta/favoritos/{cooperativaId}")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public ResponseEntity<Void> desfavoritar(Principal p, @PathVariable UUID cooperativaId) {
        service.desfavoritar(p.getName(), cooperativaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/empresas-conta/materiais")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public List<MaterialResponse> materiais() {
        return service.materiais();
    }

    @GetMapping("/empresas-conta/cooperativas")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public List<CooperativaResponse> buscar(
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) String cidade,
            @RequestParam(required = false) BigDecimal quantidadeMin) {
        return service.buscar(categoriaId, cidade, quantidadeMin);
    }

    @GetMapping("/empresas-conta/cooperativas/{id}")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public PerfilPublico publico(@PathVariable UUID id) {
        return service.publico(id);
    }

    @GetMapping("/empresas-conta/pedidos")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public List<PedidoResponse> pedidos(Principal p) {
        return service.pedidos(p.getName());
    }

    @PostMapping("/empresas-conta/pedidos")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public PedidoResponse enviar(Principal p, @RequestBody @Valid EnviarPedido b) {
        return service.enviar(p.getName(), b);
    }

    @GetMapping("/empresas-conta/pedidos/{id}")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public PedidoResponse pedido(Principal p, @PathVariable UUID id) {
        return service.pedido(p.getName(), id);
    }

    @GetMapping("/empresas-conta/pedidos/{id}/itens")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public List<ItemResponse> itens(Principal p, @PathVariable UUID id) {
        return service.itens(p.getName(), id);
    }

    @GetMapping("/empresas-conta/meus-pedidos/{id}/cooperativas")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public List<PedidoCooperativaResponse> vinculos(Principal p, @PathVariable UUID id) {
        return service.vinculos(p.getName(), id);
    }

    @GetMapping("/empresas-conta/cooperativas/{id}/avaliacoes")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public List<AvaliacaoPublica> avaliacoes(@PathVariable UUID id) {
        return service.avaliacoes(id);
    }

    @GetMapping("/empresas-conta/cooperativas/{id}/estrelas")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public Estrelas estrelas(@PathVariable UUID id) {
        return service.estrelas(id);
    }

    @PostMapping("/empresas-conta/avaliacoes")
    @PreAuthorize("hasRole('GESTOR_EMPRESA')")
    public AvaliacaoPublica avaliar(Principal p, @RequestBody @Valid Avaliar b) {
        return service.avaliar(p.getName(), b);
    }
}
