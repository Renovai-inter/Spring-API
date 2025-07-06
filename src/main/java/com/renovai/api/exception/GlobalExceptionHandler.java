package com.renovai.api.exception;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler {

    // DTO de erro padrão
    public record ErroResponse(
            LocalDateTime timestamp,
            int status,
            String erro,
            String mensagem,
            String caminho
    ) {}

    private ResponseEntity<ErroResponse> buildError(
            HttpStatus status, String erro, String mensagem, WebRequest request) {
        return ResponseEntity.status(status).body(new ErroResponse(
                LocalDateTime.now(),
                status.value(),
                erro,
                mensagem,
                request.getDescription(false).replace("uri=", "")
        ));
    }

    // 404 — Recurso não encontrado
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleRecursoNaoEncontrado(
            RecursoNaoEncontradoException ex, WebRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "Não Encontrado", ex.getMessage(), request);
    }

    // 422 — Regra de negócio violada
    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> handleRegraDeNegocio(
            RegraDeNegocioException ex, WebRequest request) {
        return buildError(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de Negócio", ex.getMessage(), request);
    }

    // 400 — Validação de campos (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> campos = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String campo = ((FieldError) error).getField();
            String mensagem = error.getDefaultMessage();
            campos.put(campo, mensagem);
        });

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 400);
        body.put("erro", "Validação de Campos");
        body.put("mensagem", "Existem erros nos campos enviados.");
        body.put("campos", campos);
        body.put("caminho", request.getDescription(false).replace("uri=", ""));

        return ResponseEntity.badRequest().body(body);
    }

    // 409 — Violação de integridade de dados
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, WebRequest request) {
        String msg = "Violação de integridade de dados. Verifique relacionamentos e campos únicos.";
        return buildError(HttpStatus.CONFLICT, "Conflito de Dados", msg, request);
    }

    // 401 — Não autenticado
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponse> handleAuthentication(
            AuthenticationException ex, WebRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, "Não Autenticado", ex.getMessage(), request);
    }

    // 403 — Sem permissão
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> handleAccessDenied(
            AccessDeniedException ex, WebRequest request) {
        return buildError(HttpStatus.FORBIDDEN, "Acesso Negado",
                "Você não tem permissão para acessar este recurso.", request);
    }

    // 500 — Erros genéricos
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGeneric(Exception ex, WebRequest request) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Erro Interno",
                "Ocorreu um erro inesperado. Contate o suporte.", request);
    }
}
