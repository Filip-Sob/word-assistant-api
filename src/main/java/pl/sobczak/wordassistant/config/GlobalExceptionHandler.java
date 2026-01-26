package pl.sobczak.wordassistant.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(String error) {}

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErrorResponse> handleWebClient(WebClientResponseException ex) {
        // Przekazujemy dalej status z OpenAI (np. 429), zamiast robić 500.
        String body = ex.getResponseBodyAsString();
        String msg = "OpenAI error: HTTP " + ex.getStatusCode().value() +
                (body == null || body.isBlank() ? "" : " body=" + body);

        return ResponseEntity
                .status(ex.getStatusCode())
                .body(new ErrorResponse(msg));
    }
}
