package pl.sobczak.wordassistant.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AssistController {

    @PostMapping("/assist")
    public AssistResponse assist(@RequestBody AssistRequest request) {
        String input = request.text() == null ? "" : request.text();
        String result = "Odpowiedź testowa z backendu:\n" + input;
        return new AssistResponse(result);
    }

    public record AssistRequest(String text) {}
    public record AssistResponse(String result) {}
}
