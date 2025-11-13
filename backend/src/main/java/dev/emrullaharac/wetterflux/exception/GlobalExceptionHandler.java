package dev.emrullaharac.wetterflux.exception;

import dev.emrullaharac.wetterflux.model.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponseDto>  handleApiException(
            ApiException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = ex.getStatus();
        return ResponseEntity.status(status)
                .body(buildError(status, ex.getMessage(), request));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String msg = "Invalid value for parameter '" + ex.getName() + "'";
        return ResponseEntity.status(status)
                .body(buildError(status, msg, request));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDto> handleMissingParam(
            MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String msg = "Missing required parameter '" + ex.getParameterName() + "'";
        return ResponseEntity.status(status)
                .body(buildError(status, msg, request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleException(
            Exception ex,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String msg = "Unexpected Server Error";
        return ResponseEntity.status(status)
                .body(buildError(status, msg, request));
    }

    private ErrorResponseDto buildError(HttpStatus status, String message, HttpServletRequest request) {
        return ErrorResponseDto.builder()
                .timestamp(Instant.now().toString())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .build();
    }
}
