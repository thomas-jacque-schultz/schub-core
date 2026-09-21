package schultz.thomas.schub.core.team.business.service;

import schultz.thomas.schub.core.team.api.dto.StatComparisonDto;
import schultz.thomas.schub.core.team.api.dto.StatLineDto;

import java.util.List;

/**
 * Les ratios, calculés au dernier moment sur des sommes.
 *
 * <p>C'est ce qui rend « le reste du pool » exact : le reste, c'est le total moins le groupe, et
 * une soustraction de sommes est juste là où une soustraction de moyennes ne veut rien dire.</p>
 */
final class StatLines {

    private StatLines() {
    }

    static StatLineDto of(RiotStatsGateway.Bucket bucket, String label, String iconUrl,
                          StatComparisonDto versusRest) {
        double minutes = bucket.secondsPlayed() / 60.0;
        return new StatLineDto(
                bucket.key(),
                label,
                iconUrl,
                bucket.games(),
                bucket.wins(),
                winRate(bucket),
                kda(bucket),
                parPartie(bucket.kills(), bucket.games()),
                parPartie(bucket.deaths(), bucket.games()),
                parPartie(bucket.assists(), bucket.games()),
                parMinute(bucket.minionsKilled(), minutes),
                parMinute(bucket.goldEarned(), minutes),
                parMinute(bucket.damageToChampions(), minutes),
                parMinute(bucket.visionScore(), minutes),
                bucket.afkGames(),
                bucket.secondsPlayed(),
                bucket.firstPlayedAt(),
                bucket.lastPlayedAt(),
                versusRest);
    }

    /**
     * Le groupe comparé au reste des parties du même joueur. Nulle quand le groupe est tout ce
     * qu'on a : se comparer à rien ne donne pas un écart de zéro.
     */
    static StatComparisonDto versusRest(RiotStatsGateway.Bucket groupe, RiotStatsGateway.Bucket total) {
        if (total == null || total.games() <= groupe.games()) {
            return null;
        }
        RiotStatsGateway.Bucket reste = soustrait(total, groupe);
        return new StatComparisonDto(reste.games(), ecart(winRate(groupe), winRate(reste)),
                ecart(kda(groupe), kda(reste)));
    }

    static RiotStatsGateway.Bucket soustrait(RiotStatsGateway.Bucket total, RiotStatsGateway.Bucket part) {
        return new RiotStatsGateway.Bucket(
                total.puuid(), "", null,
                total.games() - part.games(),
                total.wins() - part.wins(),
                total.kills() - part.kills(),
                total.deaths() - part.deaths(),
                total.assists() - part.assists(),
                total.minionsKilled() - part.minionsKilled(),
                total.goldEarned() - part.goldEarned(),
                total.damageToChampions() - part.damageToChampions(),
                total.visionScore() - part.visionScore(),
                total.afkGames() - part.afkGames(),
                total.secondsPlayed() - part.secondsPlayed(),
                total.firstPlayedAt(), total.lastPlayedAt());
    }

    static RiotStatsGateway.Bucket vide(String puuid) {
        return new RiotStatsGateway.Bucket(puuid, "", null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                null, null);
    }

    static Double winRate(RiotStatsGateway.Bucket bucket) {
        return bucket.games() == 0 ? null : (double) bucket.wins() / bucket.games();
    }

    /** Zéro mort ne divise pas : le total des kills et assists est la lecture retenue. */
    static Double kda(RiotStatsGateway.Bucket bucket) {
        if (bucket.games() == 0) {
            return null;
        }
        long positif = bucket.kills() + bucket.assists();
        return bucket.deaths() == 0 ? (double) positif : (double) positif / bucket.deaths();
    }

    static Double parMinute(long total, double minutes) {
        return minutes <= 0 ? null : total / minutes;
    }

    static Double parPartie(long total, long games) {
        return games == 0 ? null : (double) total / games;
    }

    static Double ecart(Double valeur, Double reference) {
        return valeur == null || reference == null ? null : valeur - reference;
    }

    static Double moyenne(List<Double> valeurs) {
        List<Double> connues = valeurs.stream().filter(java.util.Objects::nonNull).toList();
        return connues.isEmpty() ? null
                : connues.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
    }
}
