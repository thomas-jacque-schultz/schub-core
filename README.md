# schub-core

Domaine Schub : identité, serveurs de jeu et politique de ports, équipes League of Legends. Orchestre les connecteurs.

## Place dans l'architecture

Voir `Schub/docs/migration-microservices.md` pour la vue d'ensemble.

| | |
|---|---|
| Paquet racine | `schultz.thomas.schub.core` |
| Image Docker | `thomasschultzschub/schub-core` |
| Port en dev | 18083 (vers 8080 dans le conteneur) |
| Base Mongo | `servers` |

## Authentification

Tous les appels exigent l'en-tête `X-Internal-Secret`, sauf `GET /actuator/health`.

## Lancer en local

```
mvn spring-boot:run
```

En dev, le service est monté par `schub-infra-docker/Hosting/Tool/CodeInfrastructure/docker-compose.dev.yml`
avec les sources en volume : `task dev` depuis ce dossier, puis `task logs -- schub-core`.
