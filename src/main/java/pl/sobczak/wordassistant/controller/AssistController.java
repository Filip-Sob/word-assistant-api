package pl.sobczak.wordassistant.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import pl.sobczak.wordassistant.openai.OpenAiService;

@RestController
@RequestMapping("/api")
public class AssistController {

    private final OpenAiService openAiService;

    public AssistController(OpenAiService openAiService) {
        this.openAiService = openAiService;
    }

    @PostMapping("/assist")
    public AssistResponse assist(@RequestBody AssistRequest request) {
        String input = request.text() == null ? "" : request.text();

        try {
            String result = openAiService.assist(input);
            return new AssistResponse(result, false, null);
        } catch (WebClientResponseException.TooManyRequests ex) {
            // Fallback (MVP) – pozwala kontynuować rozwój frontu, mimo limitów OpenAI
            String fallback = """
                    --- Word Assistant (tryb testowy) ---
                    OpenAI zwróciło 429 (limit / billing / rate limit).
                    Zwracam odpowiedź zastępczą, żeby flow Word → API → Word działał.

                    Tekst wejściowy:
                    %s

                    Sugestia (mock):
                    Uporządkuj styl, skróć zdania i usuń powtórzenia.
                    """.formatted(input);

            return new AssistResponse(fallback, true, "OPENAI_429");
        }
    }

    public record AssistRequest(String text) {}

    /**
     * fallback=true oznacza, że nie dostaliśmy prawdziwej odpowiedzi AI,
     * tylko zastępczą (np. z powodu limitów).
     */
    public record AssistResponse(String result, boolean fallback, String reason) {}
}
