package de.tum.in.www1.artemis.config;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.sentry.Sentry;

@Configuration
@Profile("prod")
public class SentryConfiguration {

    private final Logger log = LoggerFactory.getLogger(SentryConfiguration.class);

    @Value("${server.url}")
    private String ARTEMIS_SERVER_URL;

    @Value("${artemis.version}")
    private String VERSION;

    @Value("${info.sentry.dsn}")
    private String SENTRY_DSN;

    /**
     * init sentry with the correct package name and Artemis version
     */
    @PostConstruct
    public void init() {
        final String dsn = SENTRY_DSN + "?stacktrace.app.packages=de.tum.in.www1.artemis";
        log.info("Sentry DSN: " + dsn);

        if (SENTRY_DSN == null) {
            return;
        }

        Sentry.init(options -> {
            options.setDsn(dsn);
            options.setEnvironment(getEnvironment());
            options.setRelease(VERSION);
        });
    }

    private String getEnvironment() {
        return switch (ARTEMIS_SERVER_URL) {
            case "https://artemis.ase.in.tum.de" -> "prod";
            case "https://artemistest.ase.in.tum.de" -> "test";
            case "https://artemistest2.ase.in.tum.de" -> "test";
            case "https://vmbruegge60.in.tum.de" -> "test";
            default -> "local";
        };
    }
}
