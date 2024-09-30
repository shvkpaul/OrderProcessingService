package org.shvk.orderprocessingservice.model;


import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

public record ErrorResponse(

        @Schema(description = "Error message", example = "Error message")
        String message,
        @Schema(description = "Http status message", example = "Http status message")
        HttpStatus httpStatus) {
}
