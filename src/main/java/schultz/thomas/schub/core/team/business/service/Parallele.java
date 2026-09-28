package schultz.thomas.schub.core.team.business.service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

// Les appels au connecteur attendent le réseau : un thread virtuel chacun, et l'exception d'origine remonte telle quelle.
final class Parallele {

    private static final ExecutorService VIRTUELS = Executors.newVirtualThreadPerTaskExecutor();

    private Parallele() {
    }

    static <V> CompletableFuture<V> lance(Supplier<V> calcul) {
        return CompletableFuture.supplyAsync(calcul, VIRTUELS);
    }

    static <K, V> Map<K, V> parCle(Collection<K> cles, Function<K, V> calcul) {
        Map<K, CompletableFuture<V>> futurs = new LinkedHashMap<>();
        cles.forEach(cle -> futurs.put(cle, lance(() -> calcul.apply(cle))));
        Map<K, V> resultats = new LinkedHashMap<>();
        futurs.forEach((cle, futur) -> resultats.put(cle, attend(futur)));
        return resultats;
    }

    static <V> V attend(CompletableFuture<V> futur) {
        try {
            return futur.join();
        } catch (CompletionException echec) {
            if (echec.getCause() instanceof RuntimeException origine) {
                throw origine;
            }
            throw echec;
        }
    }
}
