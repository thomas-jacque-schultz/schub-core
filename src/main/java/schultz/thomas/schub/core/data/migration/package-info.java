/**
 * Migrations de schéma de la base {@code servers}, pilotées par Mongock.
 *
 * <h2>Les règles</h2>
 * <ul>
 *   <li><strong>Une migration ne touche que la base {@code servers}.</strong> Le cœur se
 *       connecte avec l'utilisateur {@code servers}, qui n'a de droits que sur elle. Une
 *       ChangeUnit qui viserait une autre base serait refusée par Mongo — et c'est tant mieux :
 *       cette frontière est ce que la découpe en services achète.</li>
 *   <li><strong>Une ChangeUnit ne se modifie jamais après avoir tourné.</strong> Mongock trace
 *       son {@code id} en base et ne la rejoue pas. La corriger sur une base déjà migrée est
 *       sans effet et fait diverger les environnements. Pour corriger, on en ajoute une
 *       nouvelle.</li>
 *   <li><strong>{@code order} croissant, jamais réutilisé</strong>, et le nom de classe le
 *       reprend ({@code V001_}, {@code V002_}…) pour que l'ordre se lise dans l'arborescence.</li>
 *   <li><strong>{@code @RollbackExecution} est obligatoire</strong> — Mongock refuse de démarrer
 *       sans. Quand un retour en arrière n'a pas de sens, le dire en commentaire plutôt que de
 *       laisser la méthode vide sans explication.</li>
 * </ul>
 *
 * <h2>Ce qui n'a pas sa place ici</h2>
 * <p>Le déplacement historique {@code discordbot.servers} vers {@code servers.servers}
 * (phase 3) traverse deux bases. Le cœur n'a pas le droit de lire {@code discordbot} et ne doit
 * pas l'obtenir : ce droit ne servirait qu'une fois, au basculement de la prod, et resterait
 * acquis pour toujours. Ce déplacement reste donc une opération d'exploitation lancée à la main
 * en {@code root} — {@code mongo-migrate-servers-to-core.js} dans le repo d'infra. C'est le
 * dernier script manuel du système : tout ce qui suit passe par ce paquet.</p>
 */
package schultz.thomas.schub.core.data.migration;
