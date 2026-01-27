package pl.sobczak.wordassistant.openai;

import pl.sobczak.wordassistant.controller.dto.AssistMode;
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

    public String assist(String contextText, String instruction, AssistMode mode) {
        String context = (contextText == null) ? "" : contextText.trim();
        String instr = (instruction == null) ? "" : instruction.trim();
        AssistMode m = (mode == null) ? AssistMode.REWRITE : mode;

        // Minimalne walidacje MVP
        if (instr.isBlank()) {
            throw new IllegalArgumentException("instruction is required");
        }

        // EXPLAIN wymaga tekstu do interpretacji
        if (m == AssistMode.EXPLAIN && context.isBlank()) {
            throw new IllegalArgumentException("contextText is required for EXPLAIN mode");
        }

        // DOCUMENT celowo NIE wymaga contextu:
        // - pusty dokument => generowanie od zera
        // - niepusty => praca na całości

        String system = switch (m) {
            case REWRITE -> """
                    You are a helpful writing assistant for Microsoft Word.
                    If TEXT is provided, apply the INSTRUCTION to that text and return ONLY the modified text.
                    If TEXT is empty, generate new content that follows the INSTRUCTION and return ONLY the generated text.
                    Do not add commentary unless explicitly requested.
                    """;
            case EXPLAIN -> """
                    You are a helpful writing assistant for Microsoft Word.
                    Explain/interpret the provided TEXT according to the INSTRUCTION.
                    Return ONLY the explanation (not the original text), in a clear and structured way.
                    """;
            case DOCUMENT -> """
                    You are a helpful writing assistant for Microsoft Word.
                    You will receive the FULL DOCUMENT as TEXT (it may be empty).
                    Apply the INSTRUCTION to the whole document and return ONLY the revised full document text.
                    If the document is empty, generate new content that follows the INSTRUCTION.
                    Do not add commentary unless explicitly requested.
                    """;
        };

        String input = """
                INSTRUCTION:
                %s

                TEXT:
                %s
                """.formatted(instr, context);

        var request = new OpenAiResponsesRequest(
                model,
                system,
                input,
                0.2
        );

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

        // Fallback parser:
        // 1) preferuje content.type == "output_text"
        // 2) jeśli brak, bierze pierwszy niepusty content.text niezależnie od typu
        // 3) jeśli nadal brak tekstu => zwraca pusty string (pozwala na "delete" use-case)
        return response.output().stream()
                .flatMap(out -> out.content() == null ? Stream.empty() : out.content().stream())
                .filter(c -> c.text() != null && !c.text().isBlank())
                .sorted((a, b) -> {
                    boolean aPref = "output_text".equals(a.type());
                    boolean bPref = "output_text".equals(b.type());
                    return Boolean.compare(bPref, aPref); // output_text first
                })
                .map(c -> c.text().trim())
                .findFirst()
                .orElse("");
    }
}
