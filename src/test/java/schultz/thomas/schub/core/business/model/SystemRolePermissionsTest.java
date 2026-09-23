package schultz.thomas.schub.core.business.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SystemRolePermissionsTest {

    private static final Set<Permission> PORTEES_PAR_EQUIPE =
            EnumSet.of(Permission.TEAM_VIEW, Permission.TEAM_EDIT, Permission.COMPOSITION_EDIT);

    @Test
    @DisplayName("administrer l'hébergement ne donne aucun droit sur les équipes des autres")
    void administratorNAAucunDroitDEquipeGlobal() {
        assertThat(SystemRole.ADMINISTRATOR.permissions())
                .as("arbitrage du 21-09 : ADMINISTRATOR est un rôle de serveurs, sans lien avec le domaine LoL")
                .doesNotContainAnyElementsOf(PORTEES_PAR_EQUIPE);
    }

    @Test
    @DisplayName("la charge de la collecte est réservée à OWNER")
    void chargeDeCollecteReserveeAOwner() {
        for (SystemRole role : SystemRole.values()) {
            if (role != SystemRole.OWNER) {
                assertThat(role.permissions())
                        .as("%s ne doit pas voir la charge de la collecte", role)
                        .doesNotContain(Permission.INGEST_VIEW);
            }
        }
    }

    @Test
    @DisplayName("mais un administrateur reste une personne, qui monte son équipe")
    void administratorPeutCreerSonEquipe() {
        assertThat(SystemRole.ADMINISTRATOR.permissions()).contains(Permission.TEAM_CREATE);
    }

    @Test
    @DisplayName("seul OWNER porte tout, y compris ce qui n'existe pas encore")
    void ownerSeulPorteTout() {
        assertThat(SystemRole.OWNER.permissions()).containsExactlyInAnyOrderElementsOf(
                EnumSet.allOf(Permission.class));

        for (SystemRole role : SystemRole.values()) {
            if (role != SystemRole.OWNER) {
                assertThat(role.permissions())
                        .as("%s ne doit pas porter toutes les permissions", role)
                        .isNotEqualTo(EnumSet.allOf(Permission.class));
            }
        }
    }

    // Échoue dès qu'une permission est ajoutée à l'enum : c'est voulu.
    @Test
    @DisplayName("ajouter une permission oblige à décider qui la reçoit")
    void touteNouvellePermissionExigeUnArbitrage() {
        Set<Permission> connues = EnumSet.of(
                Permission.SERVER_VIEW, Permission.SERVER_INFRA_VIEW,
                Permission.SERVER_START, Permission.SERVER_STOP,
                Permission.SERVER_CREATE, Permission.SERVER_EDIT, Permission.SERVER_DELETE,
                Permission.PORT_VIEW, Permission.PORT_RULE_EDIT,
                Permission.DISCORD_CHANNEL_MANAGE,
                Permission.USER_VIEW, Permission.USER_ROLE_ASSIGN,
                Permission.ROLE_MANAGE,
                Permission.INGEST_VIEW,
                Permission.TEAM_CREATE, Permission.TEAM_VIEW, Permission.TEAM_EDIT,
                Permission.COMPOSITION_EDIT);

        assertThat(EnumSet.allOf(Permission.class))
                .as("une permission neuve : décide à quels rôles système elle va, puis ajoute-la ici")
                .containsExactlyInAnyOrderElementsOf(connues);
    }
}
