package schultz.thomas.schub.core.team.business.service;

import schultz.thomas.schub.core.team.api.dto.EarlyGameDto;
import schultz.thomas.schub.core.team.api.dto.TeamEarlyGameDto;
import schultz.thomas.schub.core.team.data.model.TeamMember;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class EarlyGames {

    static final String HAUT = "TOP";
    static final String BAS = "BOT";
    static final String EQUILIBRE = "BALANCED";
    private static final String CONTRE = "COUNTER";

    private EarlyGames() {
    }

    static EarlyGameDto vue(RiotStatsGateway.EarlyGame early, int notreCote, Map<String, TeamMember> parPuuid) {
        if (early == null) {
            return null;
        }
        List<EarlyGameDto.GankDto> ganks = early.ganks().stream()
                .map(gank -> {
                    boolean nous = gank.attackerSide() == notreCote;
                    return new EarlyGameDto.GankDto(gank.second(), gank.lane(), nous, gank.outcome(), gank.decisive(),
                            gank.objectiveFollowUp(), membres(gank.targetPuuids(), parPuuid),
                            membres(gank.casualtyPuuids(), parPuuid),
                            nous ? gank.attackersLost() : gank.defendersLost(),
                            nous ? gank.defendersLost() : gank.attackersLost());
                })
                .toList();
        return new EarlyGameDto(ganks,
                presence(jungler(early, notreCote, true), parPuuid),
                presence(jungler(early, notreCote, false), parPuuid),
                objectifs(early, notreCote, true),
                objectifs(early, notreCote, false));
    }

    static TeamEarlyGameDto bilan(List<RiotStatsGateway.SharedMatch> parties,
                                  Map<String, RiotStatsGateway.Insight> insights,
                                  Map<String, TeamMember> parPuuid, Map<String, String> noms) {
        Map<String, Membre> parMembre = new LinkedHashMap<>();
        parPuuid.forEach((puuid, membre) -> parMembre.put(puuid, new Membre(membre)));
        Map<String, Cote> parCote = new LinkedHashMap<>();
        List.of(HAUT, BAS, EQUILIBRE).forEach(cote -> parCote.put(cote, new Cote()));
        int analysees = 0;

        for (RiotStatsGateway.SharedMatch partie : parties) {
            RiotStatsGateway.Insight insight = insights.get(partie.matchId());
            if (insight == null || insight.early() == null) {
                continue;
            }
            analysees++;
            RiotStatsGateway.EarlyGame early = insight.early();
            int notreCote = TeamGamesStatsService.cote(partie);
            for (RiotStatsGateway.InsightPlayer joueur : insight.participants()) {
                Membre membre = parMembre.get(joueur.puuid());
                if (membre != null) {
                    membre.ajoute(joueur, early);
                }
            }
            RiotStatsGateway.JunglePresence notreJungler = jungler(early, notreCote, true);
            Membre junglerMembre = notreJungler == null ? null : parMembre.get(notreJungler.puuid());
            if (junglerMembre != null) {
                for (RiotStatsGateway.InsightPlayer joueur : insight.participants()) {
                    Membre coequipier = parMembre.get(joueur.puuid());
                    Integer minutes = minutesDuCote(notreJungler, joueur.position());
                    if (coequipier != null && coequipier != junglerMembre && joueur.side() == notreCote
                            && minutes != null) {
                        junglerMembre.aupres(coequipier.membre.getMemberId(), minutes, notreJungler);
                    }
                }
            }
            String fort = notreJungler == null ? EQUILIBRE : coteFort(notreJungler);
            parCote.get(fort).ajoute(partie, early, notreCote, fort);
        }

        return new TeamEarlyGameDto(analysees,
                parMembre.values().stream().map(membre -> membre.dto(noms)).toList(),
                parCote.entrySet().stream()
                        .filter(entree -> entree.getValue().parties > 0)
                        .map(entree -> entree.getValue().dto(entree.getKey()))
                        .toList());
    }

    // Deux minutes d'écart au moins : en deçà, le jungler a joué les deux côtés.
    static String coteFort(RiotStatsGateway.JunglePresence presence) {
        if (presence.topMinutes() >= presence.botMinutes() + 2) {
            return HAUT;
        }
        return presence.botMinutes() >= presence.topMinutes() + 2 ? BAS : EQUILIBRE;
    }

    private static Integer minutesDuCote(RiotStatsGateway.JunglePresence presence, String poste) {
        return switch (poste == null ? "" : poste) {
            case "TOP" -> presence.topMinutes();
            case "MIDDLE" -> presence.midMinutes();
            case "BOTTOM", "UTILITY" -> presence.botMinutes();
            default -> null;
        };
    }

    private static RiotStatsGateway.JunglePresence jungler(RiotStatsGateway.EarlyGame early, int notreCote,
                                                           boolean notre) {
        return early.junglers().stream()
                .filter(presence -> (presence.side() == notreCote) == notre)
                .findFirst()
                .orElse(null);
    }

    private static EarlyGameDto.JunglePresenceDto presence(RiotStatsGateway.JunglePresence presence,
                                                           Map<String, TeamMember> parPuuid) {
        if (presence == null) {
            return null;
        }
        TeamMember membre = parPuuid.get(presence.puuid());
        return new EarlyGameDto.JunglePresenceDto(membre == null ? null : membre.getMemberId(),
                presence.topMinutes(), presence.midMinutes(), presence.botMinutes(), coteFort(presence));
    }

    private static EarlyGameDto.ObjectivesDto objectifs(RiotStatsGateway.EarlyGame early, int notreCote,
                                                        boolean notre) {
        return early.objectives().stream()
                .filter(o -> (o.side() == notreCote) == notre)
                .findFirst()
                .map(o -> new EarlyGameDto.ObjectivesDto(o.dragons(), o.grubs(), o.heralds()))
                .orElse(new EarlyGameDto.ObjectivesDto(0, 0, 0));
    }

    private static List<String> membres(List<String> puuids, Map<String, TeamMember> parPuuid) {
        return puuids == null ? List.of() : puuids.stream()
                .map(parPuuid::get)
                .filter(Objects::nonNull)
                .map(TeamMember::getMemberId)
                .toList();
    }

    private static final class Membre {
        private final TeamMember membre;
        private int couloirs;
        private int subis;
        private int tenus;
        private int morts;
        private int jungles;
        private int faits;
        private int decisifs;
        private int contres;
        private int haut;
        private int milieu;
        private int bas;
        private final Map<String, int[]> aupres = new LinkedHashMap<>();

        Membre(TeamMember membre) {
            this.membre = membre;
        }

        void aupres(String coequipier, int minutes, RiotStatsGateway.JunglePresence presence) {
            int[] cumul = aupres.computeIfAbsent(coequipier, id -> new int[3]);
            cumul[0]++;
            cumul[1] += minutes;
            cumul[2] += presence.topMinutes() + presence.midMinutes() + presence.botMinutes();
        }

        void ajoute(RiotStatsGateway.InsightPlayer joueur, RiotStatsGateway.EarlyGame early) {
            String puuid = joueur.puuid();
            if ("JUNGLE".equals(joueur.position())) {
                jungles++;
                for (RiotStatsGateway.Gank gank : early.ganks()) {
                    if (puuid.equals(gank.junglerPuuid())) {
                        faits++;
                        decisifs += gank.decisive() ? 1 : 0;
                        contres += CONTRE.equals(gank.outcome()) ? 1 : 0;
                    }
                }
                early.junglers().stream().filter(p -> puuid.equals(p.puuid())).findFirst().ifPresent(p -> {
                    haut += p.topMinutes();
                    milieu += p.midMinutes();
                    bas += p.botMinutes();
                });
                return;
            }
            couloirs++;
            for (RiotStatsGateway.Gank gank : early.ganks()) {
                if (gank.attackerSide() != joueur.side() && gank.targetPuuids().contains(puuid)) {
                    subis++;
                    if (gank.casualtyPuuids().contains(puuid)) {
                        morts++;
                    } else {
                        tenus++;
                    }
                }
            }
        }

        TeamEarlyGameDto.MemberEarlyDto dto(Map<String, String> noms) {
            return new TeamEarlyGameDto.MemberEarlyDto(membre.getMemberId(), noms.get(membre.getMemberId()),
                    couloirs, subis, tenus, morts, jungles, faits, decisifs, contres, haut, milieu, bas,
                    aupres.entrySet().stream()
                            .map(e -> new TeamEarlyGameDto.PresenceWithDto(e.getKey(), noms.get(e.getKey()),
                                    e.getValue()[0], e.getValue()[1], e.getValue()[2]))
                            .toList());
        }
    }

    private static final class Cote {
        private int parties;
        private int victoires;
        private int ganksAdverses;
        private int surCoteFaible;

        void ajoute(RiotStatsGateway.SharedMatch partie, RiotStatsGateway.EarlyGame early, int notreCote,
                    String fort) {
            parties++;
            victoires += Boolean.TRUE.equals(partie.win()) ? 1 : 0;
            String faible = HAUT.equals(fort) ? BAS : BAS.equals(fort) ? HAUT : null;
            for (RiotStatsGateway.Gank gank : early.ganks()) {
                if (gank.attackerSide() != notreCote) {
                    ganksAdverses++;
                    surCoteFaible += gank.lane().equals(faible) ? 1 : 0;
                }
            }
        }

        TeamEarlyGameDto.StrongSideDto dto(String cote) {
            return new TeamEarlyGameDto.StrongSideDto(cote, parties, victoires, ganksAdverses, surCoteFaible);
        }
    }
}
