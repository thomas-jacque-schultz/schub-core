package schultz.thomas.schub.core.team.business.model;

/** Pourquoi une colonne de statistiques est vide — une colonne vide sans raison se lit comme une panne. */
public enum StatsState {

    STATISTIQUES_CONNUES,
    COMPTE_RIOT_ABSENT,
    INGESTION_EN_COURS,
    AUCUNE_PARTIE,
    EFFECTIF_INCOMPLET,
    CONNECTEUR_INDISPONIBLE
}
