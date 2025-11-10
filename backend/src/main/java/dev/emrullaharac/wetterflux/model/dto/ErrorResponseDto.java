package dev.emrullaharac.wetterflux.model.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

@AllArgsConstructor
@Builder
@Getter
@With
public class ErrorResponseDto {

    private String message;


}
