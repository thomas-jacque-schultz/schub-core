package schultz.thomas.schub.core.team.business.service;

import schultz.thomas.schub.core.team.api.dto.StatComparisonDto;
import schultz.thomas.schub.core.team.api.dto.StatLineDto;

import java.time.Instant;
import java.util.List;

// Ratios calculés au dernier moment sur des sommes : « le reste » = total − groupe n'a de sens qu'en sommes.
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
                parMinute(bucket.damageTaken(), minutes),
                parMinute(bucket.visionScore(), minutes),
                part(bucket.kills() + bucket.assists(), bucket.teamKills()),
                part(bucket.deaths(), bucket.teamDeaths()),
                bucket.afkGames(),
                bucket.secondsPlayed(),
                bucket.firstPlayedAt(),
                bucket.lastPlayedAt(),
                versusRest);
    }

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
                total.damageTaken() - part.damageTaken(),
                total.visionScore() - part.visionScore(),
                total.teamKills() - part.teamKills(),
                total.teamDeaths() - part.teamDeaths(),
                total.afkGames() - part.afkGames(),
                total.secondsPlayed() - part.secondsPlayed(),
                total.firstPlayedAt(), total.lastPlayedAt());
    }

    static RiotStatsGateway.Bucket vide(String puuid) {
        return new RiotStatsGateway.Bucket(puuid, "", null, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                null, null);
    }

    static RiotStatsGateway.Bucket additionne(String puuid, String key, List<RiotStatsGateway.Bucket> parts) {
        RiotStatsGateway.Bucket somme = new RiotStatsGateway.Bucket(puuid, key, null, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, null, null);
        for (RiotStatsGateway.Bucket part : parts) {
            somme = new RiotStatsGateway.Bucket(puuid, key, null,
                    somme.games() + part.games(),
                    somme.wins() + part.wins(),
                    somme.kills() + part.kills(),
                    somme.deaths() + part.deaths(),
                    somme.assists() + part.assists(),
                    somme.minionsKilled() + part.minionsKilled(),
                    somme.goldEarned() + part.goldEarned(),
                    somme.damageToChampions() + part.damageToChampions(),
                    somme.damageTaken() + part.damageTaken(),
                    somme.visionScore() + part.visionScore(),
                    somme.teamKills() + part.teamKills(),
                    somme.teamDeaths() + part.teamDeaths(),
                    somme.afkGames() + part.afkGames(),
                    somme.secondsPlayed() + part.secondsPlayed(),
                    plusTot(somme.firstPlayedAt(), part.firstPlayedAt()),
                    plusTard(somme.lastPlayedAt(), part.lastPlayedAt()));
        }
        return somme;
    }

    private static Instant plusTot(Instant a, Instant b) {
        return a == null ? b : b == null ? a : a.isBefore(b) ? a : b;
    }

    private static Instant plusTard(Instant a, Instant b) {
        return a == null ? b : b == null ? a : a.isAfter(b) ? a : b;
    }

    static Double winRate(RiotStatsGateway.Bucket bucket) {
        return bucket.games() == 0 ? null : (double) bucket.wins() / bucket.games();
    }

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

    static Double part(long siens, long equipe) {
        return equipe <= 0 ? null : (double) siens / equipe;
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
