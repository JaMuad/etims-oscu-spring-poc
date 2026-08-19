package com.muad.etims.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps the JSON body returned by the KRA GavaConnect token endpoint.
 *
 * <p>Typical success payload:
 * <pre>
 * {
 *   "access_token": "eyJ...",
 *   "token_type":   "Bearer",
 *   "expires_in":   3600
 * }
 * </pre>
 *
 * {@code @JsonIgnoreProperties(ignoreUnknown = true)} is intentional — KRA
 * occasionally adds undocumented fields to their responses without notice.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KraAuthResponse(

        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("expires_in")
        long expiresIn
) {}
