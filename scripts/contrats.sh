#!/bin/sh
# Recopie les contrats OpenAPI des services appelés, depuis leurs dépôts voisins (Schub#124).
# Le build du cœur génère ses modèles de fil depuis ces copies : un contrat qui change casse la compilation ici.
set -eu
ici=$(cd "$(dirname "$0")/.." && pwd)
voisins=${CONTRATS_DEPUIS:-$ici/..}
cp "$voisins/schub-connector-riot/openapi/connector-riot.json" "$ici/src/main/openapi/connector-riot.json"
echo "contrat du connecteur Riot recopié"
