package pl.sobczak.wordassistant.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import pl.sobczak.wordassistant.history.AiActionLog;
import pl.sobczak.wordassistant.history.AiActionLogRepository;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class HistoryController {

    private final AiActionLogRepository repo;

    public HistoryController(AiActionLogRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/history")
    public List<AiActionLog> history(@RequestParam String clientId,
                                     @RequestParam(defaultValue = "30") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return repo.findByClientIdOrderByCreatedAtDesc(clientId, PageRequest.of(0, safeLimit));
    }

    @GetMapping("/history/{id}")
    public AiActionLog historyOne(@PathVariable UUID id,
                                  @RequestParam String clientId) {
        return repo.findByIdAndClientId(id, clientId)
                .orElseThrow(() -> new IllegalArgumentException("log not found"));
    }
}
