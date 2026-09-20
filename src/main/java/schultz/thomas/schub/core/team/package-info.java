/**
 * Le domaine d'équipe : équipes, effectifs, compositions préparées.
 *
 * <h2>Un paquet du cœur, et pas un huitième service</h2>
 *
 * <p>À cinq joueurs et un site personnel, un service de plus coûte un dépôt, une base, une entrée
 * dans {@code release.yml}, un client HTTP, deux entrées de compose et un tag d'image à bumper —
 * pour aucun gain de disponibilité ni de montée en charge. Ce qui compte n'est pas de l'extraire
 * un jour, c'est que l'extraction reste <strong>bon marché</strong> le jour où elle se justifiera
 * (plan §D.2).</p>
 *
 * <h2>Les deux interdits qui la gardent bon marché</h2>
 *
 * <ol>
 *   <li><strong>Aucun dépôt ni entité d'ici n'est lu depuis le domaine hébergement, et
 *       réciproquement.</strong> Pas de {@code GameServerRepository} dans ce paquet, pas de
 *       {@code TeamRepository} en dehors. Le contrôle est un {@code grep}, pas un outil : il est
 *       dans la description de la PR du lot D.4 et se refait en dix secondes.</li>
 *   <li><strong>Le seul lien avec l'identité est un id.</strong> {@code TeamMember.userId}
 *       référence un {@code User} — pas de {@code @DBRef}, pas de jointure Mongo, pas de champ
 *       recopié. Tout ce qui a besoin de <em>plus</em> qu'un id passe par
 *       {@link schultz.thomas.schub.core.team.business.service.MemberDirectory}, qui est le seul
 *       point de contact avec le domaine de l'identité, et qui devient un appel HTTP le jour de
 *       l'extraction sans que rien d'autre ne bouge.</li>
 * </ol>
 *
 * <h2>Les trois couches</h2>
 *
 * <p>{@code api → business → data}, comme le reste du cœur (migration §2 bis). Le paquet racine
 * est à part, de sorte que l'extraction soit un déplacement de répertoire et un changement de
 * {@code groupId}, pas une chasse aux imports.</p>
 *
 * <h2>Ce que ce lot ne fait pas</h2>
 *
 * <p>Ni ingestion de parties (lot D.7), ni statistiques (D.8/D.9) : le cœur ne stocke
 * <strong>aucune</strong> partie, elles vivent une seule fois dans {@code connector-riot}
 * (plan §D.2 ter). Deux copies de la même donnée, ce sont deux vérités et une divergence
 * garantie.</p>
 */
package schultz.thomas.schub.core.team;
