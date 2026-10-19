/**
 * Trois couches dans chaque domaine (la plateforme ici, augur, lol.*) :
 * - api : ce qui entre et sort — contrôleurs, DTO, clients HTTP vers les autres services, mappage DTO ↔ modèle ;
 * - business : la logique, sans DTO ni HTTP ; un appel sortant passe par une interface que api implémente ;
 * - data : le modèle persisté et ses dépôts, qui ne connaissent ni business ni api.
 * Entre services, seul api se partage. config, à la racine, voit les trois couches. ArchitectureTest tient ces règles.
 */
package schultz.thomas.schub.core;
