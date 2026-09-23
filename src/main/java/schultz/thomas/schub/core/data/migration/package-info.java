/**
 * - Une migration ne touche que la base servers (seuls droits de l'utilisateur Mongo servers).
 * - Une ChangeUnit ne se modifie jamais après avoir tourné : en ajouter une nouvelle.
 * - order croissant, jamais réutilisé, repris dans le nom de classe (V001_…).
 * - @RollbackExecution obligatoire.
 * - Pas de transactions (Mongo sans replica set) : écrire des migrations idempotentes.
 * Le déplacement discordbot → servers reste manuel, en root (mongo-migrate-servers-to-core.js, repo d'infra).
 */
package schultz.thomas.schub.core.data.migration;
