package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.SystemRole;
import schultz.thomas.schub.core.business.service.GameServerService;
import schultz.thomas.schub.core.business.service.PermissionEvaluator;
import schultz.thomas.schub.core.business.service.RiotIdResolver;
import schultz.thomas.schub.core.data.model.Role;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.data.repository.RoleRepository;
import schultz.thomas.schub.core.data.repository.UserRepository;
import schultz.thomas.schub.core.team.business.model.GameRole;
import schultz.thomas.schub.core.team.business.model.MemberStatus;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;
import schultz.thomas.schub.core.team.data.repository.CompositionRepository;
import schultz.thomas.schub.core.team.data.repository.TeamRepository;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le domaine d'équipe branché sur le <em>vrai</em> évaluateur de permissions.
 *
 * <p>Les mocks s'arrêtent aux dépôts : l'autorisation, elle, passe par
 * {@link PermissionEvaluator} et {@link TeamScopedAuthority} pour de bon. C'est le seul montage
 * qui vérifie ce qui compte — qu'un non-capitaine se fasse refuser <em>par la chaîne réelle</em>,
 * et pas par un {@code when(...).thenReturn(false)} qui ne prouverait rien.</p>
 */
class TeamServiceTest {

    private TeamRepository teamRepository;
    private RoleRepository roleRepository;
    private CompositionRepository compositionRepository;
    private MemberDirectory memberDirectory;
    private RiotIdResolver riotIdResolver;
    private TeamService teamService;

    private User capitaine;
    private User membre;
    private User etranger;

    @BeforeEach
    void setUp() {
        teamRepository = mock(TeamRepository.class);
        compositionRepository = mock(CompositionRepository.class);
        memberDirectory = mock(MemberDirectory.class);
        riotIdResolver = mock(RiotIdResolver.class);

        UserRepository userRepository = mock(UserRepository.class);
        roleRepository = mock(RoleRepository.class);
        GameServerService gameServerService = mock(GameServerService.class);
        when(gameServerService.findBySlug(anyString())).thenReturn(Optional.empty());

        Role visiteur = new Role();
        visiteur.setId("role-visiteur");
        visiteur.setName(SystemRole.VISITEUR.roleName());
        visiteur.setPermissions(EnumSet.copyOf(SystemRole.VISITEUR.permissions()));
        when(roleRepository.findById("role-visiteur")).thenReturn(Optional.of(visiteur));

        Role sansDroits = new Role();
        sansDroits.setId("role-muet");
        sansDroits.setName("MUET");
        sansDroits.setPermissions(EnumSet.noneOf(Permission.class));
        when(roleRepository.findById("role-muet")).thenReturn(Optional.of(sansDroits));

        PermissionEvaluator evaluator = new PermissionEvaluator(userRepository, roleRepository,
                gameServerService, List.of(new TeamScopedAuthority(teamRepository)));

        teamService = new TeamService(teamRepository, compositionRepository, evaluator,
                memberDirectory, riotIdResolver);

        capitaine = compte("capitaine", "role-visiteur");
        membre = compte("membre", "role-visiteur");
        etranger = compte("etranger", "role-visiteur");

        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team team = invocation.getArgument(0);
            if (team.getId() == null) {
                team.setId("equipe-1");
            }
            return team;
        });
        when(riotIdResolver.resolvePuuid(anyString(), anyString())).thenReturn(Optional.empty());
        when(memberDirectory.byRiotPuuid(anyString())).thenReturn(Optional.empty());
    }

    // --- création ---

    @Test
    @DisplayName("un VISITEUR crée une équipe — sans ça, l'outil n'a aucun utilisateur")
    void creationParUnVisiteur() {
        Team equipe = teamService.create(capitaine, "  Les cinq  ");

        assertThat(equipe.getName()).isEqualTo("Les cinq");
        assertThat(equipe.getCreatedBy()).isEqualTo("capitaine");
        assertThat(equipe.getMembers()).isEmpty();
    }

    @Test
    @DisplayName("sans TEAM_CREATE, pas d'équipe")
    void creationRefusee() {
        User muet = compte("muet", "role-muet");

        assertThatThrownBy(() -> teamService.create(muet, "Les cinq"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("TEAM_CREATE");
        verify(teamRepository, never()).save(any(Team.class));
    }

    @Test
    @DisplayName("une équipe a un nom")
    void nomObligatoire() {
        assertThatThrownBy(() -> teamService.create(capitaine, "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- qui écrit ---

    @Test
    @DisplayName("un membre ne renomme pas l'équipe de son capitaine")
    void unMembreNeRenommePas() {
        donneLEquipe();

        assertThatThrownBy(() -> teamService.rename(membre, "equipe-1", "Autre nom"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("TEAM_EDIT");
    }

    @Test
    @DisplayName("un étranger ne voit même pas l'équipe")
    void unEtrangerNeVoitPas() {
        donneLEquipe();

        assertThatThrownBy(() -> teamService.requireVisible(etranger, "equipe-1"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("TEAM_VIEW");
    }

    @Test
    @DisplayName("un membre voit l'équipe")
    void unMembreVoit() {
        donneLEquipe();

        assertThat(teamService.requireVisible(membre, "equipe-1").getName()).isEqualTo("Les cinq");
    }

    @Test
    @DisplayName("le capitaine renomme son équipe")
    void leCapitaineRenomme() {
        donneLEquipe();

        assertThat(teamService.rename(capitaine, "equipe-1", "Renommée").getName()).isEqualTo("Renommée");
    }

    @Test
    @DisplayName("un OWNER renomme n'importe quelle équipe — par son rôle, pas par sa place")
    void lOwnerRenommeAussi() {
        donneLEquipe();
        Role ownerRole = new Role();
        ownerRole.setId("role-owner");
        ownerRole.setName(SystemRole.OWNER.roleName());
        ownerRole.setPermissions(EnumSet.copyOf(SystemRole.OWNER.permissions()));
        when(roleRepository.findById("role-owner")).thenReturn(Optional.of(ownerRole));

        assertThat(teamService.rename(compte("owner", "role-owner"), "equipe-1", "Renommée par le patron")
                .getName()).isEqualTo("Renommée par le patron");
    }

    @Test
    @DisplayName("un membre ne retire personne de l'effectif")
    void unMembreNeRetirePersonne() {
        donneLEquipe();

        assertThatThrownBy(() -> teamService.removeMember(membre, "equipe-1", "m-1"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("supprimer une équipe emporte ses compositions")
    void suppressionEmporteLesCompositions() {
        donneLEquipe();

        teamService.delete(capitaine, "equipe-1");

        verify(compositionRepository).deleteByTeamId("equipe-1");
        verify(teamRepository).delete(any(Team.class));
    }

    // --- effectif : lié ou libre ---

    @Test
    @DisplayName("on ajoute un joueur qui n'a pas de compte Schub — il reste libre")
    void ajouteUnMembreLibre() {
        donneLEquipe();

        Team equipe = teamService.addMember(capitaine, "equipe-1",
                new TeamService.NewMember("Bibi", "EUW", null, GameRole.MID, MemberStatus.TITULAIRE));

        TeamMember ajoute = equipe.getMembers().get(equipe.getMembers().size() - 1);
        assertThat(ajoute.isLinked()).isFalse();
        assertThat(ajoute.getUserId()).isNull();
        assertThat(ajoute.riotId()).isEqualTo("Bibi#EUW");
        assertThat(ajoute.getMemberId()).isNotBlank();
    }

    @Test
    @DisplayName("un Riot ID déjà revendiqué par un compte donne un membre lié d'emblée")
    void ajouteUnMembreDejaInscrit() {
        donneLEquipe();
        when(riotIdResolver.resolvePuuid("Bibi", "EUW")).thenReturn(Optional.of("puuid-bibi"));
        when(memberDirectory.byRiotPuuid("puuid-bibi")).thenReturn(Optional.of(
                new MemberDirectory.MemberIdentity("bibi", "Bibi", null, "puuid-bibi", "Bibi", "EUW")));

        Team equipe = teamService.addMember(capitaine, "equipe-1",
                new TeamService.NewMember("Bibi", "EUW", null, GameRole.MID, null));

        TeamMember ajoute = equipe.getMembers().get(equipe.getMembers().size() - 1);
        assertThat(ajoute.isLinked()).isTrue();
        assertThat(ajoute.getUserId()).isEqualTo("bibi");
        assertThat(ajoute.getStatus()).isEqualTo(MemberStatus.TITULAIRE);
    }

    @Test
    @DisplayName("le puuid fourni par l'appelant évite l'appel au connecteur Riot")
    void puuidFourniEviteLAppel() {
        donneLEquipe();

        teamService.addMember(capitaine, "equipe-1",
                new TeamService.NewMember("Bibi", "EUW", "puuid-bibi", GameRole.ADC, null));

        verify(riotIdResolver, never()).resolvePuuid(anyString(), anyString());
    }

    @Test
    @DisplayName("un Riot ID incomplet est refusé — un pseudo seul ne désigne personne")
    void riotIdIncomplet() {
        donneLEquipe();

        assertThatThrownBy(() -> teamService.addMember(capitaine, "equipe-1",
                new TeamService.NewMember("Bibi", "  ", null, GameRole.TOP, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Riot ID");
    }

    @Test
    @DisplayName("deux fois le même joueur dans la même équipe, non")
    void refuseUnDoublon() {
        donneLEquipe();
        teamService.addMember(capitaine, "equipe-1",
                new TeamService.NewMember("Bibi", "EUW", null, GameRole.MID, null));

        assertThatThrownBy(() -> teamService.addMember(capitaine, "equipe-1",
                new TeamService.NewMember("bibi", "euw", null, GameRole.TOP, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("déjà");
    }

    @Test
    @DisplayName("un coach ne tient pas de poste")
    void refuseUnCoachAvecPoste() {
        donneLEquipe();

        assertThatThrownBy(() -> teamService.addMember(capitaine, "equipe-1",
                new TeamService.NewMember("Coach", "EUW", null, GameRole.SUP, MemberStatus.COACH)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("coach");
    }

    @Test
    @DisplayName("une équipe n'est pas limitée à cinq — un roster a des remplaçants et un coach")
    void pasDeLimiteACinq() {
        donneLEquipe();
        for (int i = 0; i < 6; i++) {
            teamService.addMember(capitaine, "equipe-1",
                    new TeamService.NewMember("Joueur" + i, "EUW", null, null, MemberStatus.REMPLACANT));
        }
        Team equipe = teamService.addMember(capitaine, "equipe-1",
                new TeamService.NewMember("Coach", "EUW", null, null, MemberStatus.COACH));

        assertThat(equipe.getMembers()).hasSize(8);
    }

    // --- revendication ---

    @Test
    @DisplayName("un membre libre devient lié quand la personne revendique son Riot ID")
    void revendicationParRiotId() {
        Team equipe = equipe();
        equipe.getMembers().add(membreLibre("m-libre", "Bibi", "EUW", null));
        when(teamRepository.findAll()).thenReturn(List.of(equipe));

        User bibi = compte("bibi", "role-visiteur");
        bibi.setRiotGameName("bibi");
        bibi.setRiotTagLine("euw");

        List<Team> liees = teamService.claim(bibi);

        assertThat(liees).hasSize(1);
        TeamMember place = liees.get(0).findMember("m-libre").orElseThrow();
        assertThat(place.isLinked()).isTrue();
        assertThat(place.getUserId()).isEqualTo("bibi");
        assertThat(place.getLinkedAt()).isNotNull();
    }

    @Test
    @DisplayName("la revendication par puuid l'emporte, et complète le puuid manquant")
    void revendicationParPuuid() {
        Team equipe = equipe();
        equipe.getMembers().add(membreLibre("m-libre", "AncienPseudo", "EUW", "puuid-bibi"));
        when(teamRepository.findAll()).thenReturn(List.of(equipe));

        User bibi = compte("bibi", "role-visiteur");
        bibi.setRiotPuuid("puuid-bibi");
        bibi.setRiotGameName("NouveauPseudo");
        bibi.setRiotTagLine("EUW");

        List<Team> liees = teamService.claim(bibi);

        assertThat(liees).hasSize(1);
        assertThat(liees.get(0).findMember("m-libre").orElseThrow().getUserId()).isEqualTo("bibi");
    }

    @Test
    @DisplayName("on ne revendique jamais la place de quelqu'un d'autre")
    void neVolePasUnePlaceDejaLiee() {
        Team equipe = equipe();
        TeamMember deja = membreLibre("m-pris", "Bibi", "EUW", null);
        deja.setUserId("quelquun-dautre");
        equipe.getMembers().add(deja);
        when(teamRepository.findAll()).thenReturn(List.of(equipe));

        User bibi = compte("bibi", "role-visiteur");
        bibi.setRiotGameName("Bibi");
        bibi.setRiotTagLine("EUW");

        assertThat(teamService.claim(bibi)).isEmpty();
        assertThat(equipe.findMember("m-pris").orElseThrow().getUserId()).isEqualTo("quelquun-dautre");
    }

    @Test
    @DisplayName("revendiquer deux fois ne change rien la seconde fois")
    void revendicationIdempotente() {
        Team equipe = equipe();
        equipe.getMembers().add(membreLibre("m-libre", "Bibi", "EUW", null));
        when(teamRepository.findAll()).thenReturn(List.of(equipe));

        User bibi = compte("bibi", "role-visiteur");
        bibi.setRiotGameName("Bibi");
        bibi.setRiotTagLine("EUW");

        assertThat(teamService.claim(bibi)).hasSize(1);
        assertThat(teamService.claim(bibi)).isEmpty();
    }

    @Test
    @DisplayName("sans compte Riot lié, il n'y a rien à revendiquer — et on le dit")
    void revendicationSansCompteRiot() {
        assertThatThrownBy(() -> teamService.claim(etranger))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Riot");
    }

    // --- « mes équipes » ---

    @Test
    @DisplayName("mes équipes : celles où je figure et celles que j'ai créées, sans doublon")
    void mesEquipes() {
        Team equipe = equipe();
        when(teamRepository.findByMembersUserId("capitaine")).thenReturn(List.of(equipe));
        when(teamRepository.findByCreatedBy("capitaine")).thenReturn(List.of(equipe));

        assertThat(teamService.mine(capitaine)).containsExactly(equipe);
    }

    @Test
    @DisplayName("une équipe créée sans s'y mettre reste visible de son capitaine")
    void equipeCreeeSansSYMettre() {
        Team equipe = equipe();
        when(teamRepository.findByMembersUserId("capitaine")).thenReturn(List.of());
        when(teamRepository.findByCreatedBy("capitaine")).thenReturn(List.of(equipe));

        assertThat(teamService.mine(capitaine)).containsExactly(equipe);
    }

    // --- montage ---

    private void donneLEquipe() {
        when(teamRepository.findById("equipe-1")).thenReturn(Optional.of(equipe()));
    }

    private Team equipe() {
        Team team = new Team();
        team.setId("equipe-1");
        team.setName("Les cinq");
        team.setCreatedBy("capitaine");

        TeamMember leMembre = new TeamMember();
        leMembre.setMemberId("m-1");
        leMembre.setUserId("membre");
        leMembre.setRiotGameName("Membre");
        leMembre.setRiotTagLine("EUW");
        leMembre.setStatus(MemberStatus.TITULAIRE);
        team.getMembers().add(leMembre);
        return team;
    }

    private TeamMember membreLibre(String memberId, String gameName, String tagLine, String puuid) {
        TeamMember member = new TeamMember();
        member.setMemberId(memberId);
        member.setRiotGameName(gameName);
        member.setRiotTagLine(tagLine);
        member.setRiotPuuid(puuid);
        member.setStatus(MemberStatus.TITULAIRE);
        return member;
    }

    private User compte(String id, String roleId) {
        User user = new User();
        user.setId(id);
        user.setDiscordId("discord-" + id);
        user.setRoleId(roleId);
        return user;
    }
}
