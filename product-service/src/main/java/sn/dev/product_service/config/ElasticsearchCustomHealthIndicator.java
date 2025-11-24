package sn.dev.product_service.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URI;

/**
 * Health indicator personnalisé pour Elasticsearch qui évite les problèmes
 * de compatibilité de version avec le client Java officiel.
 */
@Slf4j
@Component("elasticsearch")
public class ElasticsearchCustomHealthIndicator implements HealthIndicator {

    @Value("${elasticsearch.host:localhost}")
    private String host;

    @Value("${elasticsearch.port:9200}")
    private int port;

    @Override
    public Health health() {
        try {
            // Test de connexion simple via HTTP
            URI uri = new URI("http://" + host + ":" + port + "/");
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                return Health.up()
                        .withDetail("host", host)
                        .withDetail("port", port)
                        .withDetail("status", "Connected")
                        .build();
            } else {
                return Health.down()
                        .withDetail("host", host)
                        .withDetail("port", port)
                        .withDetail("status", "HTTP " + responseCode)
                        .build();
            }
        } catch (Exception e) {
            log.warn("Elasticsearch health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("host", host)
                    .withDetail("port", port)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}

