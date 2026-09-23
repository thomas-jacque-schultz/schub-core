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

// Pas de @Scheduled(fixedRateString) : il n'accepte que des ms ou de l'ISO-8601 (PT60S), pas « 60s ».
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
