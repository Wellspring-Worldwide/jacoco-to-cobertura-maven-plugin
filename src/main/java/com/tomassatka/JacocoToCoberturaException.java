package com.tomassatka;

public class JacocoToCoberturaException extends RuntimeException {

    public JacocoToCoberturaException(String message) {
        super(message);
    }

    public JacocoToCoberturaException(String message, Throwable cause) {
        super(message, cause);
    }
}
