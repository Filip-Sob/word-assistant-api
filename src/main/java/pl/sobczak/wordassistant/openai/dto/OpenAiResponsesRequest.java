package pl.sobczak.wordassistant.openai.dto;

public record OpenAiResponsesRequest(
        String model,
        String instructions,
        String input,
        double temperature
) {}
