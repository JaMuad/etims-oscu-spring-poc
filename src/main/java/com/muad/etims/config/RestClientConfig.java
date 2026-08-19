package com.muad.etims.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Produces the application-wide {@link RestClient} bean, pre-configured with
 * the KRA GavaConnect base URL sourced from {@code application.yml}
 * (which in turn reads from the {@code .env} file via spring-dotenv).
 *
 * <p>A single shared {@link RestClient} instance is safe for concurrent use —
 * it is immutable after construction. Individual request builders created by
 * {@code restClient.post()} / {@code restClient.get()} are NOT shared.
 */
@Configuration
public class RestClientConfig {

    @Value("${kra.api.base-url}")
    private String kraBaseUrl;

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
                .baseUrl(kraBaseUrl)
                .defaultHeader("Accept", "application/json")
                .build();
    }
}
