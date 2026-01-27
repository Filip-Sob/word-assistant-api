package pl.sobczak.wordassistant.controller;

import org.springframework.web.bind.annotation.*;
import pl.sobczak.wordassistant.controller.dto.AssistRequest;
import pl.sobczak.wordassistant.controller.dto.AssistResponse;
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
        String answer = openAiService.assist(
                request.contextText(),
                request.instruction(),
                request.mode()
        );
        return new AssistResponse(answer);
    }
}
