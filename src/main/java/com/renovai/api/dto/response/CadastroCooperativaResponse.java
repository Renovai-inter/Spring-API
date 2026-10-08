package com.renovai.api.dto.response;

import java.util.UUID;

public record CadastroCooperativaResponse(
        UUID cooperativaId, UUID perfilId, UUID enderecoId, String email, String role) {}
