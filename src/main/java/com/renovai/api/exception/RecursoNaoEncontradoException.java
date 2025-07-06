package com.renovai.api.exception;

public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String recurso, Object id) {
        super(String.format("%s com id '%s' não encontrado.", recurso, id));
    }
}
