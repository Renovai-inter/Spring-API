package com.renovai.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class EmpresaContaRequests {
    public record Cadastro(
            @NotBlank @Size(max = 255) String nome,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank String cpf,
            @NotBlank @Size(max = 20) String telefone,
            @NotBlank @Size(min = 6, max = 72) String senha,
            @NotBlank @Size(max = 255) String nomeEmpresa,
            @NotBlank String cnpj,
            @NotBlank @Size(max = 255) String endereco,
            UUID materialId) {}

    public record Atualizar(
            @Size(max = 255) String nomeEmpresa,
            @Email @Size(max = 255) String email,
            String cnpj,
            @Size(max = 255) String endereco,
            @Size(max = 20) String telefone,
            String descricao,
            @Size(max = 100) String cidade) {}

    public record AlterarSenha(
            @NotBlank String senhaAtual, @NotBlank @Size(min = 6, max = 72) String novaSenha) {}

    public record InteresseRequest(@NotNull UUID categoriaId) {}

    public record SubstituirInteresses(@NotNull @Size(max = 100) List<@NotNull UUID> categoriaIds) {}

    public record FavoritoContaRequest(@NotNull UUID cooperativaId) {}

    public record PedidoItem(
            @NotNull UUID materialId,
            @NotNull @DecimalMin("0.001") @Digits(integer = 7, fraction = 3)
                    BigDecimal quantidadeKg,
            @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2)
                    BigDecimal precoUnitario) {}

    public record EnviarPedido(
            @NotNull UUID cooperativaId,
            @NotNull UUID chaveSolicitacao,
            @NotEmpty @Size(max = 100) List<@NotNull @Valid PedidoItem> itens) {}

    public record Avaliar(
            @NotNull UUID pedidoId,
            @NotNull UUID cooperativaId,
            @NotNull @Min(1) @Max(5) Integer nota,
            @Size(max = 4000) String comentario) {}
}
