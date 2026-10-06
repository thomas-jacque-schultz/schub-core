package schultz.thomas.schub.core.team.api.dto;

import schultz.thomas.schub.core.team.business.model.GameRole;

import java.util.List;

// Page publique d'une équipe (Schub#98) : des constats neutres ou positifs seulement, comme les pages de joueur.
public record PublicTeamDto(
        String id,
        String name,
        List<Member> members,
        long games,
        long wins,
        long monthGames,
        long monthWins,
        BestDuo bestDuo,
        List<Champion> champions
) {

    // riotId absent : le membre a masqué son nom.
    public record Member(String riotId, String slug, List<GameRole> roles) {
    }

    public record BestDuo(String a, String b, long games, long wins, double delta) {
    }

    public record Champion(int championId, String name, String iconUrl, long games, long wins) {
    }

    public record Listed(String id, String name) {
    }
}
