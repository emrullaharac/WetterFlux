package dev.emrullaharac.wetterflux.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class ErrorResponseDto {
    private final String timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
}
