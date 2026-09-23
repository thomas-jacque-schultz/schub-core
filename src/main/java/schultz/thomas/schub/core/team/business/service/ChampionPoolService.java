package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.ChampionCatalogEntryDto;
import schultz.thomas.schub.core.team.api.dto.ChampionPoolColumnDto;
import schultz.thomas.schub.core.team.api.dto.ChampionPoolDto;
import schultz.thomas.schub.core.team.api.dto.ChampionPoolEntryDto;
import schultz.thomas.schub.core.team.api.dto.ChampionPoolMemberDto;
import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;
import schultz.thomas.schub.core.team.business.model.PoolState;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamChampionPool;
import schultz.thomas.schub.core.team.data.model.TeamMember;
import schultz.thomas.schub.core.team.data.repository.TeamChampionPoolRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChampionPoolService {

    private final TeamService teamService;
    private final MemberDirectory memberDirectory;
    private final RiotChampionGateway championGateway;
    private final RiotStatsGateway statsGateway;
    private final TeamChampionPoolRepository pools;
    private final PermissionEvaluator permissionEvaluator;

    public ChampionPoolDto of(User actor, String teamId, Integer plancherDemande) {
        Team team = teamService.requireVisible(actor, teamId);
        TeamChampionPool pool = poolDe(teamId);
        int plancher = plancherDemande == null ? pool.getMasteryFloor() : Math.max(0, plancherDemande);

        Optional<RiotChampionGateway.Catalogue> catalogue = championGateway.catalogue();
        List<TeamMember> joueurs = joueursDe(team);
        Map<String, Maitrises> maitrises = maitrisesDe(joueurs, catalogue.isPresent());
        Map<String, MemberDirectory.MemberIdentity> identites = resoutLesComptes(joueurs);
        Map<String, RiotStatsGateway.Bucket> parties = partiesParChampion(joueurs);

        List<ChampionPoolColumnDto> colonnes = new ArrayList<>();
        for (GameRole role : GameRole.values()) {
            colonnes.add(colonne(role, pool, joueurs, maitrises, identites, parties, catalogue, plancher));
        }

        return new ChampionPoolDto(
                team.getId(),
                team.getName(),
                catalogue.map(RiotChampionGateway.Catalogue::version).orElse(null),
                plancher,
                pool.getMasteryFloor(),
                catalogue.map(ChampionPoolService::catalogue).orElseGet(List::of),
                colonnes,
                placeDuLecteur(team, actor),
                peutEcrire(actor, teamId),
                Instant.now());
    }

    public ChampionPoolDto setChampions(User actor, String teamId, GameRole role, List<String> championKeys) {
        teamService.requireVisible(actor, teamId);
        permissionEvaluator.require(actor, Permission.COMPOSITION_EDIT, TeamService.ref(teamId));

        RiotChampionGateway.Catalogue catalogue = championGateway.catalogue().orElseThrow(
                () -> new IllegalStateException(
                        "Catalogue des champions indisponible : le choix ne peut pas être vérifié"));

        Set<String> connues = new HashSet<>();
        catalogue.parId().values().forEach(champion -> connues.add(champion.key()));

        Set<String> retenues = new LinkedHashSet<>();
        for (String key : championKeys == null ? List.<String>of() : championKeys) {
            String propre = key == null ? null : key.trim();
            if (propre == null || propre.isEmpty()) {
                continue;
            }
            if (!connues.contains(propre)) {
                throw new IllegalArgumentException("Aucun champion nommé « " + propre + " » dans ce patch");
            }
            retenues.add(propre);
        }
        TeamChampionPool pool = poolDe(teamId);
        pool.setChampionKeys(role, List.copyOf(retenues));
        pool.setUpdatedAt(Instant.now());
        pools.save(pool);
        log.info("Pool du poste {} de l'équipe {} : {} champion(s) retenu(s)", role, teamId, retenues.size());
        return of(actor, teamId, null);
    }

    public ChampionPoolDto setMasteryFloor(User actor, String teamId, int masteryFloor) {
        teamService.requireVisible(actor, teamId);
        permissionEvaluator.require(actor, Permission.COMPOSITION_EDIT, TeamService.ref(teamId));
        if (masteryFloor < 0) {
            throw new IllegalArgumentException("Un plancher de maîtrise ne peut pas être négatif");
        }

        TeamChampionPool pool = poolDe(teamId);
        pool.setMasteryFloor(masteryFloor);
        pool.setUpdatedAt(Instant.now());
        pools.save(pool);
        return of(actor, teamId, null);
    }

    private TeamChampionPool poolDe(String teamId) {
        return pools.findById(teamId).orElseGet(() -> {
            TeamChampionPool neuf = new TeamChampionPool();
            neuf.setTeamId(teamId);
            return neuf;
        });
    }

    private boolean peutEcrire(User actor, String teamId) {
        return permissionEvaluator.can(actor, Permission.COMPOSITION_EDIT, TeamService.ref(teamId));
    }

    private ChampionPoolColumnDto colonne(
            GameRole role,
            TeamChampionPool pool,
            List<TeamMember> joueurs,
            Map<String, Maitrises> maitrises,
            Map<String, MemberDirectory.MemberIdentity> identites,
            Map<String, RiotStatsGateway.Bucket> parties,
            Optional<RiotChampionGateway.Catalogue> catalogue,
            int plancher) {

        List<TeamMember> duPoste = joueurs.stream().filter(membre -> membre.playsRole(role)).toList();

        List<ChampionPoolMemberDto> muets = duPoste.stream()
                .filter(membre -> etat(membre, maitrises) != PoolState.MAITRISES_CONNUES)
                .map(membre -> projette(membre, maitrises, identites, null, null))
                .toList();

        if (catalogue.isEmpty()) {
            return new ChampionPoolColumnDto(role, List.of(), muets, 0);
        }

        Map<String, RiotChampionGateway.Champion> parCle = new HashMap<>();
        catalogue.get().parId().values().forEach(champion -> parCle.put(champion.key(), champion));

        List<ChampionPoolEntryDto> champions = new ArrayList<>();
        int masques = 0;
        for (String key : pool.championKeys(role)) {
            RiotChampionGateway.Champion champion = parCle.get(key);
            if (champion == null) {
                champions.add(new ChampionPoolEntryDto(0, key, null, null, List.of(), 0));
                continue;
            }
            ChampionPoolEntryDto entree = entree(champion, duPoste, maitrises, identites, parties, plancher);
            if (entree.players().isEmpty() && entree.name() != null) {
                masques++;
                continue;
            }
            champions.add(entree);
        }

        champions.sort(Comparator.comparingDouble(ChampionPoolService::maitriseMoyenne).reversed()
                .thenComparing(entree -> entree.name() == null ? "" : entree.name()));

        return new ChampionPoolColumnDto(role, champions, muets, masques);
    }

    private static double maitriseMoyenne(ChampionPoolEntryDto entree) {
        return entree.players().stream()
                .map(ChampionPoolMemberDto::masteryPoints)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0d);
    }

    private ChampionPoolEntryDto entree(
            RiotChampionGateway.Champion champion,
            List<TeamMember> duPoste,
            Map<String, Maitrises> maitrises,
            Map<String, MemberDirectory.MemberIdentity> identites,
            Map<String, RiotStatsGateway.Bucket> parties,
            int plancher) {

        List<ChampionPoolMemberDto> retenus = new ArrayList<>();
        int ecartes = 0;

        for (TeamMember membre : duPoste) {
            Maitrises siennes = maitrises.get(membre.getMemberId());
            if (siennes == null || siennes.state() != PoolState.MAITRISES_CONNUES) {
                continue;
            }
            RiotChampionGateway.Mastery maitrise = siennes.parChampion().get(champion.id());
            int points = maitrise == null ? 0 : maitrise.points();
            if (points == 0) {
                continue;
            }
            if (points < plancher) {
                ecartes++;
                continue;
            }
            retenus.add(projette(membre, maitrises, identites, maitrise,
                    parties.get(cle(membre.getRiotPuuid(), champion.id()))));
        }

        retenus.sort(Comparator.comparingInt(
                (ChampionPoolMemberDto joueur) -> joueur.masteryPoints() == null ? 0 : joueur.masteryPoints())
                .reversed());

        return new ChampionPoolEntryDto(champion.id(), champion.key(), champion.name(),
                champion.iconUrl(), retenus, ecartes);
    }

    private ChampionPoolMemberDto projette(
            TeamMember membre,
            Map<String, Maitrises> maitrises,
            Map<String, MemberDirectory.MemberIdentity> identites,
            RiotChampionGateway.Mastery maitrise,
            RiotStatsGateway.Bucket joue) {

        Maitrises siennes = maitrises.get(membre.getMemberId());
        MemberDirectory.MemberIdentity identite =
                membre.getUserId() == null ? null : identites.get(membre.getUserId());

        Integer points = siennes != null && siennes.state() == PoolState.MAITRISES_CONNUES
                ? (maitrise == null ? 0 : maitrise.points())
                : null;

        return new ChampionPoolMemberDto(
                membre.getMemberId(),
                nomAffiche(membre, identite),
                identite == null ? null : identite.avatarUrl(),
                membre.getRiotGameName(),
                membre.getRiotTagLine(),
                membre.getStatus(),
                membre.isLinked(),
                etat(membre, maitrises),
                maitrise == null ? null : maitrise.level(),
                points,
                joue == null ? null : joue.games(),
                joue == null ? null : StatLines.winRate(joue),
                maitrise == null ? null : maitrise.lastPlayedAt(),
                siennes == null ? null : siennes.observedAt());
    }

    // Faille seulement, tout l'historique : le même périmètre que les champions de Mes stats.
    private Map<String, RiotStatsGateway.Bucket> partiesParChampion(List<TeamMember> joueurs) {
        List<String> puuids = joueurs.stream()
                .map(TeamMember::getRiotPuuid)
                .filter(puuid -> puuid != null && !puuid.isBlank())
                .distinct()
                .toList();
        Map<String, RiotStatsGateway.Bucket> index = new HashMap<>();
        statsGateway.aggregate(puuids, RiotStatsGateway.Grouping.CHAMPION, RiotStatsGateway.Scope.RIFT, null)
                .orElseGet(List::of)
                .forEach(bucket -> index.put(bucket.puuid() + "#" + bucket.key(), bucket));
        return index;
    }

    private static String cle(String puuid, int championId) {
        return puuid + "#" + championId;
    }

    private static PoolState etat(TeamMember membre, Map<String, Maitrises> maitrises) {
        Maitrises siennes = maitrises.get(membre.getMemberId());
        return siennes == null ? PoolState.COMPTE_RIOT_ABSENT : siennes.state();
    }

    private static List<TeamMember> joueursDe(Team team) {
        if (team.getMembers() == null) {
            return List.of();
        }
        return team.getMembers().stream()
                .filter(membre -> membre.getStatus() != MemberStatus.COACH)
                .sorted(Comparator
                        .comparing((TeamMember membre) -> membre.getStatus() != MemberStatus.TITULAIRE)
                        .thenComparing(membre -> Optional.ofNullable(membre.riotId()).orElse(""),
                                String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    // Toutes les maîtrises, pas un top N : le onzième champion retenu passerait pour « aucune maîtrise ».
    private Map<String, Maitrises> maitrisesDe(List<TeamMember> joueurs, boolean catalogueLa) {
        Map<String, Maitrises> parPuuid = new HashMap<>();
        Map<String, Maitrises> parMembre = new HashMap<>();

        for (TeamMember membre : joueurs) {
            String puuid = membre.getRiotPuuid();
            if (puuid == null || puuid.isBlank()) {
                parMembre.put(membre.getMemberId(), Maitrises.sans(PoolState.COMPTE_RIOT_ABSENT));
                continue;
            }
            if (!catalogueLa) {
                parMembre.put(membre.getMemberId(), Maitrises.sans(PoolState.CATALOGUE_INDISPONIBLE));
                continue;
            }
            parMembre.put(membre.getMemberId(),
                    parPuuid.computeIfAbsent(puuid, p -> lit(championGateway.masteries(p))));
        }
        return parMembre;
    }

    private static Maitrises lit(Optional<List<RiotChampionGateway.Mastery>> reponse) {
        if (reponse.isEmpty()) {
            return Maitrises.sans(PoolState.MAITRISES_INDISPONIBLES);
        }
        if (reponse.get().isEmpty()) {
            return Maitrises.sans(PoolState.AUCUNE_MAITRISE);
        }
        Map<Integer, RiotChampionGateway.Mastery> parChampion = new HashMap<>();
        Instant observedAt = null;
        for (RiotChampionGateway.Mastery maitrise : reponse.get()) {
            parChampion.put(maitrise.championId(), maitrise);
            if (observedAt == null) {
                observedAt = maitrise.observedAt();
            }
        }
        return new Maitrises(PoolState.MAITRISES_CONNUES, parChampion, observedAt);
    }

    private static List<ChampionCatalogEntryDto> catalogue(RiotChampionGateway.Catalogue catalogue) {
        return catalogue.parId().values().stream()
                .map(champion -> new ChampionCatalogEntryDto(
                        champion.id(), champion.key(), champion.name(), champion.iconUrl()))
                .sorted(Comparator.comparing(
                        entree -> Optional.ofNullable(entree.name()).orElse(entree.championKey()),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static String nomAffiche(TeamMember membre, MemberDirectory.MemberIdentity identite) {
        if (identite != null && identite.displayName() != null && !identite.displayName().isBlank()) {
            return identite.displayName();
        }
        return membre.riotId();
    }

    private Map<String, MemberDirectory.MemberIdentity> resoutLesComptes(List<TeamMember> joueurs) {
        Set<String> ids = new HashSet<>();
        joueurs.stream()
                .map(TeamMember::getUserId)
                .filter(id -> id != null && !id.isBlank())
                .forEach(ids::add);
        return ids.isEmpty() ? Map.of() : memberDirectory.byIds(ids);
    }

    private static String placeDuLecteur(Team team, User actor) {
        if (actor == null || actor.getId() == null || team.getMembers() == null) {
            return null;
        }
        return team.getMembers().stream()
                .filter(membre -> actor.getId().equals(membre.getUserId()))
                .map(TeamMember::getMemberId)
                .findFirst()
                .orElse(null);
    }

    private record Maitrises(PoolState state,
                             Map<Integer, RiotChampionGateway.Mastery> parChampion,
                             Instant observedAt) {

        static Maitrises sans(PoolState state) {
            return new Maitrises(state, Map.of(), null);
        }
    }
}
