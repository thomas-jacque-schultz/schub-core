package schultz.thomas.schub.core.team.business.service;

import java.util.Optional;

/**
 * {@code Pseudo#TAG} → {@code puuid}.
 *
 * <p>C'est <strong>le seul</strong> besoin que ce lot a de l'API Riot (plan : « pas d'appel à
 * {@code connector-riot} au-delà de ce qu'il faut pour résoudre un Riot ID à l'ajout d'un membre
 * libre »). Ni maîtrises, ni classements, ni parties : tout cela appartient aux lots D.5 et
 * suivants.</p>
 *
 * <p><strong>La résolution est au mieux, jamais bloquante.</strong> Un échec rend
 * {@link Optional#empty()} et le membre est ajouté sans {@code puuid} : faire dépendre la
 * création d'une équipe de la disponibilité de l'API Riot serait payer une indisponibilité
 * externe avec une fonctionnalité qui n'en a pas besoin. L'appelant peut aussi fournir le
 * {@code puuid} lui-même, auquel cas rien n'est appelé.</p>
 */
public interface RiotIdResolver {

    Optional<String> resolvePuuid(String gameName, String tagLine);
}
