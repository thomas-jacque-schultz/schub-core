package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

// « Les N derniers patchs » exprimé en jours depuis le début du plus ancien : les services ne connaissent que des jours.
@Service
@RequiredArgsConstructor
public class StatsWindows {

    private final RiotStatsGateway statsGateway;

    public Integer days(Integer days, Integer patches) {
        if (patches == null || patches <= 0) {
            return days;
        }
        List<RiotStatsGateway.PatchStart> derniers = statsGateway.patches(patches).orElseGet(List::of);
        if (derniers.isEmpty()) {
            return days;
        }
        Instant debut = derniers.getLast().startedAt();
        return (int) Math.max(1, Duration.between(debut, Instant.now()).toDays() + 1);
    }
}
