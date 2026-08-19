package com.muad.etims.service;

import com.muad.etims.dto.response.KraAuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Base64;

/**
 * Handles the OAuth 2.0 client-credentials flow with the KRA GavaConnect gateway.
 *
 * <p>The endpoint is:
 * {@code POST /token/generate?grant_type=client_credentials}
 * with a standard {@code Authorization: Basic <base64(key:secret)>} header
 * and an {@code application/x-www-form-urlencoded} body.
 *
 * <p><strong>Resilience note:</strong> KRA's sandbox returns spurious 5xx errors.
 * This service intentionally keeps the call synchronous and lets callers (or the
 * async retry queue) decide how to handle failures. Do NOT add blind retries here
 * without exponential back-off; hammering a struggling gateway makes things worse.
 */
@Service
public class KraAuthService {

    private static final Logger log = LoggerFactory.getLogger(KraAuthService.class);
    private static final String TOKEN_PATH = "/token/generate";

    private final RestClient restClient;
    private final String consumerKey;
    private final String consumerSecret;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public KraAuthService(
            RestClient restClient,
            @Value("${kra.api.consumer-key}") String consumerKey,
            @Value("${kra.api.consumer-secret}") String consumerSecret
    ) {
        this.restClient = restClient;
        this.consumerKey = consumerKey;
        this.consumerSecret = consumerSecret;
    }

    /**
     * Fetches a fresh Bearer token from KRA.
     *
     * @return {@link KraAuthResponse} containing the access token and its TTL.
     * @throws RestClientResponseException on any 4xx/5xx HTTP error from KRA.
     * @throws IllegalStateException       if the response body cannot be parsed.
     */
    public KraAuthResponse fetchToken() {
        String credentials = consumerKey + ":" + consumerSecret;
        String basicHeader = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

        // KRA expects grant_type in the query string, not the body
        MultiValueMap<String, String> formBody = new LinkedMultiValueMap<>();
        formBody.add("grant_type", "client_credentials");

        log.info("Requesting KRA OAuth token from {}{}", TOKEN_PATH, "?grant_type=client_credentials");

        try {
            // Use exchange() to get raw visibility into KRA's exact HTTP status, headers, and body
            KraAuthResponse response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(TOKEN_PATH)
                            .build())
                    .header("Authorization", basicHeader)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formBody)
                    .exchange((request, clientResponse) -> {
                        String body = new String(clientResponse.getBody().readAllBytes());
                        log.info("KRA Gateway Raw Response Status: {}", clientResponse.getStatusCode());
                        log.info("KRA Gateway Raw Response Headers: {}", clientResponse.getHeaders());
                        log.info("KRA Gateway Raw Response Body: {}", body);

                        if (clientResponse.getStatusCode().isError()) {
                            throw new IllegalStateException("KRA API Error: " + clientResponse.getStatusCode() + " - " + body);
                        }

                        try {
                            return objectMapper.readValue(body, KraAuthResponse.class);
                        } catch (Exception e) {
                            throw new IllegalStateException("Unparseable KRA auth response. Raw payload: " + body, e);
                        }
                    });

            log.info("KRA token acquired — type={}, expires_in={}s",
                    response != null ? response.tokenType() : "null",
                    response != null ? response.expiresIn() : -1);

            return response;
        } catch (Exception ex) {
            log.error("Auth request failed completely", ex);
            throw ex;
        }
    }
}
