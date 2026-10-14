package com.renovai.api.service;

import com.renovai.api.dto.response.Responses.LoginResponse;
import com.renovai.api.exception.RegraDeNegocioException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Login com Google: o app manda o ID token que recebeu do Google, a API confere esse token no
 * próprio Google e, se o e-mail for verificado e já tiver conta no Renovaí, devolve o mesmo JWT
 * do login por senha. Não cria conta nova.
 *
 * <p>Configuração: GOOGLE_CLIENT_ID = "ID do cliente da Web" do projeto Firebase/Google Cloud
 * (o mesmo usado no app como server client id). Aceita vários, separados por vírgula.
 */
@Service
public class GoogleAuthService {

    private static final String TOKENINFO = "https://oauth2.googleapis.com/tokeninfo?id_token={token}";
    private static final Set<String> EMISSORES = Set.of("accounts.google.com", "https://accounts.google.com");

    private final AuthService authService;
    private final Set<String> clientIds;
    private final RestClient http = RestClient.create();

    public GoogleAuthService(AuthService authService, @Value("${google.client-id:}") String clientIds) {
        this.authService = authService;
        this.clientIds =
                Arrays.stream(clientIds == null ? new String[0] : clientIds.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toUnmodifiableSet());
    }

    public LoginResponse login(String idToken) {
        if (clientIds.isEmpty()) {
            throw new RegraDeNegocioException("O login com Google não está configurado no servidor.");
        }
        Map<String, Object> dados;
        try {
            dados =
                    http.get()
                            .uri(TOKENINFO, idToken)
                            .retrieve()
                            .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        } catch (RestClientResponseException e) {
            throw new RegraDeNegocioException("Login do Google inválido ou expirado. Tente de novo.");
        } catch (RestClientException e) {
            throw new RegraDeNegocioException(
                    "Não foi possível confirmar o login com o Google agora. Tente de novo.");
        }
        return authService.loginVerificado(emailVerificado(dados, clientIds));
    }

    /** Confere os dados devolvidos pelo Google e devolve o e-mail confirmado. */
    static String emailVerificado(Map<String, Object> dados, Set<String> clientIds) {
        if (dados == null) {
            throw new RegraDeNegocioException("Login do Google inválido ou expirado. Tente de novo.");
        }
        String aud = texto(dados.get("aud"));
        String iss = texto(dados.get("iss"));
        String email = texto(dados.get("email"));
        boolean verificado = "true".equalsIgnoreCase(texto(dados.get("email_verified")));
        if (!clientIds.contains(aud) || !EMISSORES.contains(iss)) {
            throw new RegraDeNegocioException("Este login do Google não pertence ao app Renovaí.");
        }
        if (email.isBlank() || !verificado) {
            throw new RegraDeNegocioException("O e-mail desta conta Google não está verificado.");
        }
        return email;
    }

    private static String texto(Object valor) {
        return valor == null ? "" : String.valueOf(valor).trim();
    }
}
