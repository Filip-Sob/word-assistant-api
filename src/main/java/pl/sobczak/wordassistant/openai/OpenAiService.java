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

        if (instr.isBlank()) {
            throw new IllegalArgumentException("instruction is required");
        }

        if (m == AssistMode.EXPLAIN && context.isBlank()) {
            throw new IllegalArgumentException("contextText is required for EXPLAIN mode");
        }

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
