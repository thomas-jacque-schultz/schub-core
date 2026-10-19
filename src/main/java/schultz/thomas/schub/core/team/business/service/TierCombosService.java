package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.service.RiotConnectorUnavailableException;
import schultz.thomas.schub.core.team.api.dto.TierCombosDto;
import schultz.thomas.schub.core.team.business.model.ComboKind;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

// Les combos qui gagnent, palier par palier (Schub#112). Plancher de 20 parties : sous lui, la borne de Wilson
// classait des 7/7 devant un 74 % sur 42 parties, et la page se lisait comme une loterie.
@Service
@RequiredArgsConstructor
public class TierCombosService {

    static final List<String> PALIERS = List.of("IRON_BRONZE", "SILVER_GOLD", "PLATINUM_EMERALD", "DIAMOND",
            "MASTER_PLUS");
    static final Set<String> TYPES = Set.of(ComboKind.ADC_SUP.name(), ComboKind.JGL_MID.name(),
            ComboKind.JGL_MID_SUP.name());
    static final int PARTIES_MINIMUM = 20;
    static final int LIGNES = 30;

    private final ConnectorRiotComboGateway combos;
    private final RiotChampionGateway championGateway;

    public TierCombosDto of(String tier, String kind) {
        if (!PALIERS.contains(tier) || !TYPES.contains(kind)) {
            throw new NoSuchElementException("Aucune page de combos pour " + tier + " / " + kind);
        }
        ConnectorRiotComboGateway.TierCombos lus = combos.tierCombos(tier, kind, PARTIES_MINIMUM, LIGNES)
                .orElseThrow(RiotConnectorUnavailableException::new);
        Map<Integer, RiotChampionGateway.Champion> catalogue = championGateway.catalogue()
                .map(RiotChampionGateway.Catalogue::parId).orElseGet(Map::of);
        List<TierCombosDto.Line> lignes = lus.combos().stream()
                .map(combo -> new TierCombosDto.Line(
                        combo.championIds().stream().map(id -> champion(catalogue, id)).toList(),
                        combo.games(), combo.wins(), (double) combo.wins() / combo.games(), combo.lowerBound()))
                .toList();
        return new TierCombosDto(tier, kind, ComboKind.valueOf(kind).positions(), lus.computedAt(), lus.games(),
                PARTIES_MINIMUM, lignes);
    }

    private static TierCombosDto.Champion champion(Map<Integer, RiotChampionGateway.Champion> catalogue, int id) {
        RiotChampionGateway.Champion connu = catalogue.get(id);
        return connu == null
                ? new TierCombosDto.Champion(id, null, null, null)
                : new TierCombosDto.Champion(id, connu.key(), connu.name(), connu.iconUrl());
    }
}
