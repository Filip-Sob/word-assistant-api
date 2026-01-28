package pl.sobczak.wordassistant.controller.dto;

import pl.sobczak.wordassistant.history.ActionType;
import pl.sobczak.wordassistant.history.Scope;

public record AssistRequest(
        String clientId,
        Scope scope,
        ActionType actionType,
        String contextText,
        String instruction,
        AssistMode mode
) {}
