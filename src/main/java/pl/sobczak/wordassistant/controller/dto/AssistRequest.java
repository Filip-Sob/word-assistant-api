package pl.sobczak.wordassistant.controller.dto;

public record AssistRequest(
        String contextText,
        String instruction,
        AssistMode mode
) {}
