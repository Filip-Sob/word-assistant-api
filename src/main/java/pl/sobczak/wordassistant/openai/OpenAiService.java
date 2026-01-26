package pl.sobczak.wordassistant.openai;

import pl.sobczak.wordassistant.openai.dto.OpenAiResponsesRequest;
import pl.sobczak.wordassistant.openai.dto.OpenAiResponsesResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.stream.Stream;

@Service
public class OpenAiService {

    private final WebClient openAiWebClient;
    private final String model;

    public OpenAiService(WebClient openAiWebClient,
                         @Value("${openai.model}") String model) {
        this.openAiWebClient = openAiWebClient;
        this.model = model;
    }

    public String assist(String text) {
        String input = (text == null) ? "" : text.trim();

        var request = new OpenAiResponsesRequest(
                model,
                "You are a helpful writing assistant. Improve the text clearly and concisely.",
                input,
                0.2
        );


        // Jeśli OpenAI zwróci 4xx/5xx, WebClient rzuci WebClientResponseException,
        // a my to mapujemy na czytelny status w GlobalExceptionHandler.
        OpenAiResponsesResponse response = openAiWebClient.post()
                .uri("/responses")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OpenAiResponsesResponse.class)
                .timeout(Duration.ofSeconds(30))
                .block();

        if (response == null || response.output() == null) {
            throw new IllegalStateException("Empty response from OpenAI");
        }

        return response.output().stream()
                .flatMap(out -> out.content() == null ? Stream.empty() : out.content().stream())
                .filter(c -> "output_text".equals(c.type()))
                .map(OpenAiResponsesResponse.ContentItem::text)
                .findFirst()
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalStateException("No output_text in OpenAI response"));
    }
}
