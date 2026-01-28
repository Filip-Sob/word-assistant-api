package pl.sobczak.wordassistant.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.sobczak.wordassistant.controller.dto.AssistMode;
import pl.sobczak.wordassistant.controller.dto.AssistRequest;
import pl.sobczak.wordassistant.controller.dto.AssistResponse;
import pl.sobczak.wordassistant.history.*;
import pl.sobczak.wordassistant.openai.OpenAiService;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class AssistController {

    private final OpenAiService openAiService;
    private final AiActionLogRepository logRepository;

    public AssistController(OpenAiService openAiService, AiActionLogRepository logRepository) {
        this.openAiService = openAiService;
        this.logRepository = logRepository;
    }

    @PostMapping("/assist")
    public AssistResponse assist(@RequestBody AssistRequest request) {

        String clientId = request.clientId() == null ? "" : request.clientId().trim();
        if (clientId.isBlank()) {
            throw new IllegalArgumentException("clientId is required");
        }

        Scope scope = request.scope() == null ? Scope.SELECTION : request.scope();
        ActionType actionType = request.actionType() == null ? ActionType.OTHER : request.actionType();

        String contextText = request.contextText();
        String instruction = request.instruction();
        AssistMode mode = request.mode();

        AiActionLog log = new AiActionLog();
        log.setClientId(clientId);
        log.setScope(scope);
        log.setActionType(actionType);
        log.setPrompt(instruction == null ? "" : instruction);
        log.setInputText(contextText == null ? "" : contextText);

        try {
            String answer = openAiService.assist(contextText, instruction, mode);

            log.setOutputText(answer); // może być "" (np. delete)
            log.setStatus(Status.SUCCESS);
            log.setErrorMessage(null);

            AiActionLog saved = logRepository.save(log);
            return new AssistResponse(saved.getId(), answer);

        } catch (Exception ex) {
            log.setOutputText(null);
            log.setStatus(Status.ERROR);
            log.setErrorMessage(ex.getMessage());

            UUID id = logRepository.save(log).getId();
            throw new AssistFailedException(id, ex.getMessage());
        }
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public static class AssistFailedException extends RuntimeException {
        private final UUID logId;

        public AssistFailedException(UUID logId, String message) {
            super(message);
            this.logId = logId;
        }

        public UUID getLogId() {
            return logId;
        }
    }
}
