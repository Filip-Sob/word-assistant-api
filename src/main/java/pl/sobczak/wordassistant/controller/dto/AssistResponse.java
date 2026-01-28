package pl.sobczak.wordassistant.controller.dto;

import java.util.UUID;

public record AssistResponse(
        UUID logId,
        String answer
) {}
