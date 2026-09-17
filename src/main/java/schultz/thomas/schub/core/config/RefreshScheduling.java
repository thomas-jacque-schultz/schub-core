package schultz.thomas.schub.core.config;

import schultz.thomas.schub.core.business.service.GameServerStateRefresh;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import java.time.Duration;

/**
 * Programme la boucle de réconciliation.
 *
 * <p>Via un registrar plutôt que {@code @Scheduled(fixedRateString)} : cette annotation n'accepte
 * qu'un nombre de millisecondes ou de l'ISO-8601, ce qui imposerait d'écrire {@code PT60S} en
 * configuration au lieu de {@code 60s}.</p>
 */
@Slf4j
@EnableAsync
@EnableScheduling
@Configuration
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "core")
public class RefreshScheduling implements SchedulingConfigurer {

    private final GameServerStateRefresh gameServerStateRefresh;

    private Duration refreshInterval = Duration.ofSeconds(60);

    public void setRefreshInterval(Duration refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        log.info("Boucle de réconciliation programmée toutes les {}s", refreshInterval.toSeconds());
        registrar.addFixedRateTask(gameServerStateRefresh::refresh, refreshInterval);
    }
}
