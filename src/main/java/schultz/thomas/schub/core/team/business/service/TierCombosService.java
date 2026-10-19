package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.service.RiotConnectorUnavailableException;
import schultz.thomas.schub.core.team.api.dto.TierCombosDto;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

// Les combos qui gagnent, palier par palier (Schub#112) : les mêmes seuils de confiance que les conseils de draft.
@Service
@RequiredArgsConstructor
public class TierCombosService {

    static final List<String> PALIERS = List.of("IRON_BRONZE", "SILVER_GOLD", "PLATINUM_EMERALD", "DIAMOND",
            "MASTER_PLUS");
    static final Map<String, List<String>> TYPES = Map.of(
            "ADC_SUP", List.of("BOTTOM", "UTILITY"),
            "JGL_MID", List.of("JUNGLE", "MIDDLE"),
            "JGL_MID_SUP", List.of("JUNGLE", "MIDDLE", "UTILITY"));
    static final int PARTIES_MINIMUM = 3;
    static final int LIGNES = 30;

    private final ConnectorRiotComboGateway combos;
    private final RiotChampionGateway championGateway;

    public TierCombosDto of(String tier, String kind) {
        if (!PALIERS.contains(tier) || !TYPES.containsKey(kind)) {
            throw new NoSuchElementException("Aucune page de combos pour " + tier + " / " + kind);
        }
        ConnectorRiotComboGateway.TierCombos lus = combos.tierCombos(tier, kind, PARTIES_MINIMUM, LIGNES)
                .orElseThrow(RiotConnectorUnavailableException::new);
        Map<Integer, RiotChampionGateway.Champion> catalogue = championGateway.catalogue()
                .map(RiotChampionGateway.Catalogue::parId).orElseGet(Map::of);
        int seuil = kind.equals("JGL_MID_SUP") ? DraftAdviceService.TRIO_SEUIL : DraftAdviceService.DUO_SEUIL;
        List<TierCombosDto.Line> lignes = lus.combos().stream()
                .map(combo -> new TierCombosDto.Line(
                        combo.championIds().stream().map(id -> champion(catalogue, id)).toList(),
                        combo.games(), combo.wins(), (double) combo.wins() / combo.games(), combo.lowerBound(),
                        combo.games() < seuil))
                .toList();
        return new TierCombosDto(tier, kind, TYPES.get(kind), lus.computedAt(), lus.games(), seuil, lignes);
    }

    private static TierCombosDto.Champion champion(Map<Integer, RiotChampionGateway.Champion> catalogue, int id) {
        RiotChampionGateway.Champion connu = catalogue.get(id);
        return connu == null
                ? new TierCombosDto.Champion(id, null, null, null)
                : new TierCombosDto.Champion(id, connu.key(), connu.name(), connu.iconUrl());
    }
}
