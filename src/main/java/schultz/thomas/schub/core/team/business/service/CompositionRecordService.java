package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.business.model.CompositionRecord;
import schultz.thomas.schub.core.team.data.model.Composition;
import schultz.thomas.schub.core.team.data.model.CompositionSlot;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;
import schultz.thomas.schub.core.team.data.repository.CompositionRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Réponse du 05/10 : une partie compte dès que chaque emplacement a son champion, ou un de ses remplaçants, dans
// notre camp, quel que soit le joueur ou le poste.
@Service
@RequiredArgsConstructor
public class CompositionRecordService {

    private final TeamService teamService;
    private final RiotStatsGateway statsGateway;
    private final RiotChampionGateway championGateway;
    private final CompositionRepository compositions;

    public List<CompositionRecord> of(User actor, String teamId, Integer days) {
        Team team = teamService.requireVisible(actor, teamId);
        List<TeamMember> joueurs = TeamPlayerStatsService.joueursDe(team);
        Map<String, TeamMember> parPuuid = TeamPlayerStatsService.parPuuid(joueurs);
        List<Composition> liste = compositions.findByTeamIdOrderByUpdatedAtDesc(teamId);
        if (liste.isEmpty() || parPuuid.size() < 2) {
            return List.of();
        }
        Map<String, Integer> parCle = new HashMap<>();
        championGateway.catalogue().ifPresent(catalogue ->
                catalogue.parId().values().forEach(champion -> parCle.put(champion.key(), champion.id())));
        List<RiotStatsGateway.SharedMatch> parties = statsGateway.sharedMatches(List.copyOf(parPuuid.keySet()), 2,
                        PlayerStatsService.depuis(days), TeamGamesStatsService.PARTIES_MAX)
                .map(RiotStatsGateway.SharedMatches::matches).orElseGet(List::of).stream()
                .filter(p -> p.win() != null && !p.splitSides() && !p.players().isEmpty())
                .toList();

        List<CompositionRecord> bilans = new ArrayList<>();
        for (Composition composition : liste) {
            List<Set<Integer>> emplacements = emplacements(composition, parCle);
            if (emplacements.isEmpty()) {
                bilans.add(new CompositionRecord(composition.getId(), 0, 0, true, List.of()));
                continue;
            }
            List<CompositionRecord.Game> jouees = new ArrayList<>();
            for (RiotStatsGateway.SharedMatch partie : parties) {
                if (correspond(partie, emplacements)) {
                    jouees.add(new CompositionRecord.Game(partie.matchId(), partie.startedAt(),
                            Boolean.TRUE.equals(partie.win()), partie.durationSeconds()));
                }
            }
            long victoires = jouees.stream().filter(CompositionRecord.Game::win).count();
            bilans.add(new CompositionRecord(composition.getId(), jouees.size(), victoires,
                    jouees.size() < DraftAdviceService.TRIO_SEUIL, jouees));
        }
        return bilans;
    }

    // Un emplacement sans champion ne contraint rien ; une composition incomplète n'a pas de bilan.
    static List<Set<Integer>> emplacements(Composition composition, Map<String, Integer> parCle) {
        List<Set<Integer>> emplacements = new ArrayList<>();
        for (CompositionSlot slot : composition.getSlots()) {
            Set<Integer> champions = new HashSet<>();
            if (slot.getChampionId() != null) {
                champions.add(parCle.get(slot.getChampionId()));
            }
            if (slot.getAlternatives() != null) {
                slot.getAlternatives().forEach(cle -> champions.add(parCle.get(cle)));
            }
            champions.remove(null);
            if (!champions.isEmpty()) {
                emplacements.add(champions);
            }
        }
        return emplacements.size() == 5 ? emplacements : List.of();
    }

    // Affectation : chaque emplacement prend un champion distinct de notre camp.
    static boolean correspond(RiotStatsGateway.SharedMatch partie, List<Set<Integer>> emplacements) {
        List<Integer> nos = VueDePartie.de(partie).notreCamp().stream()
                .map(RiotStatsGateway.SharedMatchPlayer::championId).toList();
        return affecte(emplacements, 0, nos, new boolean[nos.size()]);
    }

    private static boolean affecte(List<Set<Integer>> emplacements, int i, List<Integer> nos, boolean[] pris) {
        if (i == emplacements.size()) {
            return true;
        }
        for (int j = 0; j < nos.size(); j++) {
            if (!pris[j] && emplacements.get(i).contains(nos.get(j))) {
                pris[j] = true;
                if (affecte(emplacements, i + 1, nos, pris)) {
                    return true;
                }
                pris[j] = false;
            }
        }
        return false;
    }
}
