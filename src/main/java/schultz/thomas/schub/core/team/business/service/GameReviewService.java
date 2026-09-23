package schultz.thomas.schub.core.team.business.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.api.dto.GameReviewDto;
import schultz.thomas.schub.core.team.api.dto.GameReviewsDto;
import schultz.thomas.schub.core.team.data.model.GameReview;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;
import schultz.thomas.schub.core.team.data.repository.GameReviewRepository;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameReviewService {

    private static final int CONTENU_MAX = 2000;

    private final GameReviewRepository reviewRepository;
    private final TeamService teamService;
    private final TeamGamesStatsService gamesStatsService;
    private final MemberDirectory memberDirectory;
    private final PermissionEvaluator permissionEvaluator;

    public GameReviewsDto ofGame(User actor, String teamId, String matchId) {
        Team team = teamService.requireVisible(actor, teamId);
        List<GameReview> revues =
                reviewRepository.findByTeamIdAndMatchIdOrderByCreatedAtAsc(teamId, matchId);
        return new GameReviewsDto(
                teamId,
                matchId,
                projette(revues, team, actor),
                placeDuLecteur(team, actor),
                peutNoterToutLeMonde(actor, teamId),
                Instant.now());
    }

    public GameReviewDto create(User actor, String teamId, String matchId, String subjectMemberId,
                                String content) {
        Team team = teamService.requireVisible(actor, teamId);
        TeamMember sujet = sujetValide(team, subjectMemberId);
        exigeLeDroitDeNoter(actor, team, sujet);

        if (!gamesStatsService.estPartieDEquipe(team, matchId)) {
            throw new NoSuchElementException(
                    "La partie '" + matchId + "' n'est pas une partie de cette équipe");
        }
        reviewRepository
                .findByMatchIdAndSubjectMemberIdAndAuthorUserId(matchId, sujet.getMemberId(), actor.getId())
                .ifPresent(existante -> {
                    throw new IllegalStateException(
                            "Vous avez déjà écrit une note sur ce joueur pour cette partie");
                });

        GameReview revue = new GameReview();
        revue.setTeamId(teamId);
        revue.setMatchId(matchId);
        revue.setSubjectMemberId(sujet.getMemberId());
        revue.setAuthorUserId(actor.getId());
        revue.setContent(contenuValide(content));
        revue.setCreatedAt(Instant.now());
        revue.setUpdatedAt(revue.getCreatedAt());

        GameReview creee = reviewRepository.save(revue);
        log.info("Note de revue écrite sur la partie {} de l'équipe « {} » par {}",
                matchId, team.getName(), actor.getDiscordId());
        return projette(creee, team, actor, identites(team, List.of(creee)));
    }

    public GameReviewDto update(User actor, String teamId, String matchId, String reviewId,
                                String content) {
        Team team = teamService.requireVisible(actor, teamId);
        GameReview revue = require(teamId, matchId, reviewId);
        exigeLeDroitDeRetoucher(actor, teamId, revue);

        revue.setContent(contenuValide(content));
        revue.setUpdatedAt(Instant.now());
        GameReview enregistree = reviewRepository.save(revue);
        return projette(enregistree, team, actor, identites(team, List.of(enregistree)));
    }

    public void delete(User actor, String teamId, String matchId, String reviewId) {
        teamService.requireVisible(actor, teamId);
        GameReview revue = require(teamId, matchId, reviewId);
        exigeLeDroitDeRetoucher(actor, teamId, revue);
        reviewRepository.delete(revue);
    }

    private GameReview require(String teamId, String matchId, String reviewId) {
        GameReview revue = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Aucune note d'identifiant '" + reviewId + "'"));
        if (!teamId.equals(revue.getTeamId()) || !matchId.equals(revue.getMatchId())) {
            throw new NoSuchElementException(
                    "La note '" + reviewId + "' n'appartient pas à cette partie de cette équipe");
        }
        return revue;
    }

    private TeamMember sujetValide(Team team, String subjectMemberId) {
        if (subjectMemberId == null || subjectMemberId.isBlank()) {
            throw new IllegalArgumentException("Une note désigne le joueur sur lequel elle porte");
        }
        return teamService.jouable(team, subjectMemberId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Le joueur noté n'est pas un joueur de cette équipe"));
    }

    private void exigeLeDroitDeNoter(User actor, Team team, TeamMember sujet) {
        if (sujet.getMemberId().equals(placeDuLecteur(team, actor))) {
            return;
        }
        permissionEvaluator.require(actor, Permission.TEAM_EDIT, TeamService.ref(team.getId()));
    }

    private void exigeLeDroitDeRetoucher(User actor, String teamId, GameReview revue) {
        if (actor != null && actor.getId() != null && actor.getId().equals(revue.getAuthorUserId())) {
            return;
        }
        if (!permissionEvaluator.can(actor, Permission.TEAM_EDIT, TeamService.ref(teamId))) {
            throw new AccessDeniedException("Cette note appartient à quelqu'un d'autre");
        }
    }

    private boolean peutNoterToutLeMonde(User actor, String teamId) {
        return permissionEvaluator.can(actor, Permission.TEAM_EDIT, TeamService.ref(teamId));
    }

    private List<GameReviewDto> projette(List<GameReview> revues, Team team, User actor) {
        Map<String, MemberDirectory.MemberIdentity> identites = identites(team, revues);
        return revues.stream().map(revue -> projette(revue, team, actor, identites)).toList();
    }

    private GameReviewDto projette(GameReview revue, Team team, User actor,
                                   Map<String, MemberDirectory.MemberIdentity> identites) {
        Optional<TeamMember> sujet = team.findMember(revue.getSubjectMemberId());
        Optional<TeamMember> auteur = team.getMembers() == null ? Optional.empty()
                : team.getMembers().stream()
                .filter(membre -> revue.getAuthorUserId().equals(membre.getUserId()))
                .findFirst();

        return new GameReviewDto(
                revue.getId(),
                revue.getMatchId(),
                revue.getSubjectMemberId(),
                sujet.map(membre -> nomAffiche(membre, identites)).orElse(null),
                auteur.map(TeamMember::getMemberId).orElse(null),
                nomDeLAuteur(revue, auteur, identites),
                revue.getContent(),
                revue.getCreatedAt(),
                revue.getUpdatedAt(),
                peutRetoucher(actor, team, revue));
    }

    private boolean peutRetoucher(User actor, Team team, GameReview revue) {
        if (actor == null || actor.getId() == null) {
            return false;
        }
        return actor.getId().equals(revue.getAuthorUserId())
                || permissionEvaluator.can(actor, Permission.TEAM_EDIT, TeamService.ref(team.getId()));
    }

    private Map<String, MemberDirectory.MemberIdentity> identites(Team team, List<GameReview> revues) {
        Set<String> ids = new HashSet<>();
        if (team.getMembers() != null) {
            team.getMembers().stream()
                    .map(TeamMember::getUserId)
                    .filter(id -> id != null && !id.isBlank())
                    .forEach(ids::add);
        }
        revues.stream()
                .map(GameReview::getAuthorUserId)
                .filter(id -> id != null && !id.isBlank())
                .forEach(ids::add);
        return ids.isEmpty() ? Map.of() : memberDirectory.byIds(ids);
    }

    private static String nomDeLAuteur(GameReview revue, Optional<TeamMember> auteur,
                                       Map<String, MemberDirectory.MemberIdentity> identites) {
        if (auteur.isPresent()) {
            return nomAffiche(auteur.get(), identites);
        }
        MemberDirectory.MemberIdentity identite = identites.get(revue.getAuthorUserId());
        return identite == null ? null : identite.displayName();
    }

    private static String nomAffiche(TeamMember membre,
                                     Map<String, MemberDirectory.MemberIdentity> identites) {
        MemberDirectory.MemberIdentity identite =
                membre.getUserId() == null ? null : identites.get(membre.getUserId());
        if (identite != null && identite.displayName() != null && !identite.displayName().isBlank()) {
            return identite.displayName();
        }
        return membre.riotId();
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

    private static String contenuValide(String content) {
        String propre = content == null ? "" : content.trim();
        if (propre.isEmpty()) {
            throw new IllegalArgumentException("Une note de revue n'est pas vide");
        }
        if (propre.length() > CONTENU_MAX) {
            throw new IllegalArgumentException(
                    "Une note de revue ne dépasse pas " + CONTENU_MAX + " caractères");
        }
        return propre;
    }
}
