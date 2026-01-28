package pl.sobczak.wordassistant.history;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiActionLogRepository extends JpaRepository<AiActionLog, UUID> {
    List<AiActionLog> findByClientIdOrderByCreatedAtDesc(String clientId, Pageable pageable);
    Optional<AiActionLog> findByIdAndClientId(UUID id, String clientId);
}
