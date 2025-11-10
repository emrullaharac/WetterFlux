package dev.emrullaharac.wetterflux.exception;

public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }
}
