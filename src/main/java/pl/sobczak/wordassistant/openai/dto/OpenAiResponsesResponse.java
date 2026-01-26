package pl.sobczak.wordassistant.openai.dto;

import java.util.List;

public record OpenAiResponsesResponse(List<OutputItem> output) {

    public record OutputItem(String type, List<ContentItem> content) {}

    public record ContentItem(String type, String text) {}
}
