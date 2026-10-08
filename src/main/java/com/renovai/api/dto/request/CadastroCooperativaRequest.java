package com.renovai.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

/** Cadastro institucional: o perfil criado é o administrador da cooperativa. */
public record CadastroCooperativaRequest(
        @NotBlank @Size(max = 255) String nome,
        @Size(max = 4000) String descricao,
        @Min(0) Integer numeroCooperados,
        @Size(max = 100) String horarioFuncionamento,
        @Size(max = 2048) String imagemUrl,
        @NotBlank @Size(max = 50) String contatoPreferencial,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank
                @Pattern(
                        regexp = "\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}",
                        message = "CNPJ inválido (use 00.000.000/0000-00)")
                String cnpj,
        @NotBlank @Size(min = 6, max = 72) String senha,
        @NotNull @Valid EnderecoCadastro endereco) {

    public record EnderecoCadastro(
            @NotBlank @Pattern(regexp = "\\d{5}-\\d{3}", message = "CEP inválido (use 00000-000)")
                    String cep,
            @NotBlank @Size(max = 255) String logradouro,
            @NotBlank @Size(max = 20) String numero,
            @Size(max = 255) String complemento,
            @NotBlank @Size(max = 100) String bairro,
            @NotBlank @Size(max = 100) String cidade) {}
}
