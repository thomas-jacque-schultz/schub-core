package schultz.thomas.schub.core.team.business.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import schultz.thomas.schub.core.augur.business.engine.Percentiles;
import schultz.thomas.schub.core.augur.business.engine.Signals;
import schultz.thomas.schub.core.augur.business.service.Sensors;
import schultz.thomas.schub.core.team.api.dto.ReferenceGridDto;
import schultz.thomas.schub.core.team.api.dto.StatLineDto;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// Capteurs LoL : les indicateurs d'une partie (grille GAME) et les moyennes d'un joueur (grille MEAN),
// situés en centiles dans le référentiel de leur poste et de leur palier.
@Component
@RequiredArgsConstructor
public class LolSensors implements Sensors {

    private static final Duration FRAICHEUR_REFERENTIEL = Duration.ofHours(1);
    private static final String SUPPORT = "UTILITY";

    private final RiotStatsGateway statsGateway;
    private final ObjectMapper objectMapper;
    private final Map<String, Cached> grilles = new ConcurrentHashMap<>();

    private record Cached(Optional<ReferenceGridDto> grille, Instant lu) {
    }

    @Override
    public Map<String, Signals> game(String matchId) {
        Map<String, Signals> joueurs = new LinkedHashMap<>();
        statsGateway.matchMetrics(matchId).orElseGet(List::of).forEach(joueur -> {
            Optional<ReferenceGridDto> grille = grille(joueur.position(), "GAME", joueur.tier());
            Signals signaux = contexte(joueur.position(), joueur.tier());
            joueur.values().forEach((cle, valeur) -> {
                if (valeur != null) {
                    signaux.put(cle, valeur, centile(grille, cle, joueur.tier(), valeur));
                }
            });
            joueurs.put(joueur.puuid(), signaux);
        });
        return joueurs;
    }

    @Override
    public Optional<Signals> habit(String puuid, Instant since) {
        Optional<RiotStatsGateway.Bucket> principal = statsGateway
                .aggregate(List.of(puuid), RiotStatsGateway.Grouping.POSITION, RiotStatsGateway.Scope.RIFT, since)
                .orElseGet(List::of).stream()
                .max(Comparator.comparingLong(RiotStatsGateway.Bucket::games));
        if (principal.isEmpty() || principal.get().games() == 0) {
            return Optional.empty();
        }
        String poste = principal.get().key();
        String palier = statsGateway.references(List.of(new RiotStatsGateway.ReferenceRequest(puuid, poste, since)))
                .orElseGet(List::of).stream()
                .findFirst()
                .map(RiotStatsGateway.References::tier)
                .orElse(null);
        Optional<ReferenceGridDto> grille = grille(poste, "MEAN", palier);

        StatLineDto ligne = StatLines.of(principal.get(), poste, null, null);
        Signals signaux = contexte(poste, palier).put("games", ligne.games(), null);
        objectMapper.convertValue(ligne, Map.class).forEach((cle, valeur) -> {
            if (valeur instanceof Number nombre && !"games".equals(cle)) {
                signaux.put(String.valueOf(cle), nombre.doubleValue(),
                        centile(grille, String.valueOf(cle), palier, nombre.doubleValue()));
            }
        });
        return Optional.of(signaux);
    }

    private static Signals contexte(String poste, String palier) {
        return new Signals()
                .context("position", poste)
                .context("tier", palier)
                .put("isSupport", SUPPORT.equals(poste) ? 1 : 0, null);
    }

    private static Double centile(Optional<ReferenceGridDto> grille, String metrique, String palier, double valeur) {
        return grille.map(g -> {
            ReferenceGridDto.Metric m = g.metrics() == null ? null : g.metrics().get(metrique);
            ReferenceGridDto.Tier t = m == null || m.tiers() == null || palier == null ? null : m.tiers().get(palier);
            return t == null ? null : Percentiles.of(valeur, g.percentiles(), t.values());
        }).orElse(null);
    }

    private Optional<ReferenceGridDto> grille(String poste, String portee, String palier) {
        if (poste == null || palier == null) {
            return Optional.empty();
        }
        String cle = portee + ":" + poste + ":" + palier;
        Cached lu = grilles.get(cle);
        if (lu != null && lu.lu().plus(FRAICHEUR_REFERENTIEL).isAfter(Instant.now())) {
            return lu.grille();
        }
        Optional<ReferenceGridDto> grille = statsGateway.referenceGrid(poste, portee, palier, null);
        grilles.put(cle, new Cached(grille, Instant.now()));
        return grille;
    }
}
