package schultz.thomas.schub.core.team.business.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.ResourceType;
import schultz.thomas.schub.core.data.model.User;
import schultz.thomas.schub.core.team.data.model.Team;
import schultz.thomas.schub.core.team.data.model.TeamMember;
import schultz.thomas.schub.core.team.data.repository.TeamRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Ce qu'une équipe donne, et surtout ce qu'elle ne donne pas.
 *
 * <p>La portée est la même mécanique que les {@code admins} d'un serveur : ce qui est vérifié
 * ici n'est pas qu'un capitaine peut écrire — c'est qu'un <strong>membre</strong> ne peut pas, et
 * qu'un inconnu n'obtient rien du tout. Sans ces deux refus, la portée serait décorative.</p>
 */
class TeamScopedAuthorityTest {

    private TeamRepository teamRepository;
    private TeamScopedAuthority authority;

    private User capitaine;
    private User membre;
    private User etranger;

    @BeforeEach
    void setUp() {
        teamRepository = mock(TeamRepository.class);
        authority = new TeamScopedAuthority(teamRepository);

        capitaine = compte("capitaine");
        membre = compte("membre");
        etranger = compte("etranger");

        when(teamRepository.findById(anyString())).thenReturn(Optional.empty());
        when(teamRepository.findById("equipe-1")).thenReturn(Optional.of(equipe()));
    }

    @Test
    @DisplayName("le capitaine voit, modifie l'effectif et écrit les compositions")
    void leCapitaine() {
        assertThat(authority.grantedTo(capitaine, "equipe-1")).containsExactlyInAnyOrder(
                Permission.TEAM_VIEW, Permission.TEAM_EDIT, Permission.COMPOSITION_EDIT);
    }

    @Test
    @DisplayName("un membre voit tout et n'écrit rien")
    void unMembre() {
        assertThat(authority.grantedTo(membre, "equipe-1")).containsExactly(Permission.TEAM_VIEW);
    }

    @Test
    @DisplayName("qui n'est pas de l'équipe n'en tire rien, pas même de la voir")
    void unEtranger() {
        assertThat(authority.grantedTo(etranger, "equipe-1")).isEmpty();
    }

    @Test
    @DisplayName("une équipe qui n'existe pas ne donne rien — elle ne fait pas répondre oui")
    void equipeInconnue() {
        assertThat(authority.grantedTo(capitaine, "equipe-fantome")).isEmpty();
    }

    @Test
    @DisplayName("sans acteur ni ressource, rien n'est accordé")
    void sansActeur() {
        assertThat(authority.grantedTo(null, "equipe-1")).isEmpty();
        assertThat(authority.grantedTo(capitaine, null)).isEmpty();
    }

    @Test
    @DisplayName("ne répond que pour les équipes, jamais pour un serveur")
    void nePrendPasLaPlaceDesServeurs() {
        assertThat(authority.resourceType()).isEqualTo(ResourceType.TEAM);
    }

    @Test
    @DisplayName("un membre libre ne donne aucun droit à personne — une place n'est pas un compte")
    void unMembreLibreNAutorisePersonne() {
        Team equipe = equipe();
        TeamMember libre = new TeamMember();
        libre.setMemberId("m-libre");
        libre.setRiotGameName("Invite");
        libre.setRiotTagLine("EUW");
        equipe.getMembers().add(libre);
        when(teamRepository.findById("equipe-2")).thenReturn(Optional.of(equipe));

        assertThat(authority.grantedTo(etranger, "equipe-2")).isEmpty();
    }

    private Team equipe() {
        Team team = new Team();
        team.setId("equipe-1");
        team.setName("Les cinq");
        team.setCreatedBy("capitaine");

        TeamMember leMembre = new TeamMember();
        leMembre.setMemberId("m-1");
        leMembre.setUserId("membre");
        team.setMembers(new ArrayList<>(List.of(leMembre)));
        return team;
    }

    private User compte(String id) {
        User user = new User();
        user.setId(id);
        user.setDiscordId("discord-" + id);
        return user;
    }
}
