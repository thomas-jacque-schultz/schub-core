package schultz.thomas.schub.core.data.migration;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import schultz.thomas.schub.core.business.model.Permission;
import schultz.thomas.schub.core.business.model.SystemRole;

import java.util.List;

/**
 * Retire à {@code ADMINISTRATOR} les droits d'équipe qu'il n'aurait jamais dû recevoir.
 *
 * <h2>Ce qui s'est passé</h2>
 *
 * <p>{@code ADMINISTRATOR} était défini comme « tout sauf {@code ROLE_MANAGE} ». À l'arrivée du
 * chantier D, il a donc hérité mécaniquement de {@code TEAM_VIEW}, {@code TEAM_EDIT} et
 * {@code COMPOSITION_EDIT} — et pouvait modifier l'effectif de <strong>n'importe quelle</strong>
 * équipe, y compris celle d'un autre. Arbitrage de l'utilisateur le 2026-09-21 :
 * <em>« administrator est un rôle pour les serveurs, aucun lien avec la partie Riot »</em>.</p>
 *
 * <p>{@link SystemRole} est corrigé et sa liste est devenue explicite, ce qui règle le cas d'une
 * base neuve : {@code V005} lit l'enum et n'accordera plus rien. Mais sur une base où
 * {@code V005} a <strong>déjà</strong> tourné, les trois permissions sont écrites dans la
 * collection {@code roles} et l'enum n'y peut plus rien — d'où cette migration.</p>
 *
 * <h2>Ce qu'elle ne touche pas</h2>
 *
 * <p>{@code TEAM_CREATE} reste : un administrateur est aussi une personne, qui monte son équipe
 * comme tout le monde. Et les trois permissions retirées restent obtenables <em>sur une équipe
 * donnée</em> en en étant capitaine ou membre — c'est {@code TeamScopedAuthority} qui les
 * accorde, pas le rôle. Concrètement, un administrateur ne perd aucun droit sur ses propres
 * équipes ; il perd celui d'entrer dans celles des autres.</p>
 *
 * <p>Les rôles créés à la main ne sont pas touchés : ce qui s'y trouve y a été mis exprès.</p>
 */
@ChangeUnit(id = "administrator-sans-droits-equipe", order = "006", author = "schub")
public class V006_AdministratorSansEquipes {

    private static final Logger log = LoggerFactory.getLogger(V006_AdministratorSansEquipes.class);

    /** Les trois permissions qui s'évaluent SUR une équipe, donc jamais accordées globalement. */
    private static final List<String> PORTEES_PAR_EQUIPE = List.of(
            Permission.TEAM_VIEW.name(),
            Permission.TEAM_EDIT.name(),
            Permission.COMPOSITION_EDIT.name());

    @Execution
    public void execution(MongoTemplate mongoTemplate) {
        long modifies = mongoTemplate.getCollection("roles").updateOne(
                Filters.eq("name", SystemRole.ADMINISTRATOR.roleName()),
                Updates.pullAll("permissions", PORTEES_PAR_EQUIPE)).getModifiedCount();

        if (modifies == 0) {
            log.info("ADMINISTRATOR ne portait aucun droit d'équipe global — rien à retirer.");
        } else {
            log.info("ADMINISTRATOR : {} retirée(s). Les droits sur une équipe s'obtiennent en en "
                    + "étant capitaine ou membre.", PORTEES_PAR_EQUIPE);
        }
    }

    /**
     * Les rend à {@code ADMINISTRATOR}, puisque c'est l'état d'où l'on vient.
     *
     * <p>Rétablir un droit trop large est le comportement correct d'un retour en arrière : il
     * ramène la version précédente telle qu'elle était, sans juger.</p>
     */
    @RollbackExecution
    public void rollback(MongoTemplate mongoTemplate) {
        mongoTemplate.getCollection("roles").updateOne(
                Filters.eq("name", SystemRole.ADMINISTRATOR.roleName()),
                Updates.addEachToSet("permissions", PORTEES_PAR_EQUIPE));
    }
}
