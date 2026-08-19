package com.muad.etims.controller;

import com.muad.etims.dto.response.KraAuthResponse;
import com.muad.etims.service.KraAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

/**
 * Scratch test controller — only exists to drive ad-hoc manual verification
 * of the KRA connectivity layer during PoC development.
 *
 * <p><strong>Remove or gate behind a dev profile before any production promotion.</strong>
 *
 * <p>Test with:
 * <pre>
 *   curl -s http://localhost:8080/test/kra/auth | jq .
 * </pre>
 */
@RestController
@RequestMapping("/test/kra")
public class EtimsTestController {

    private static final Logger log = LoggerFactory.getLogger(EtimsTestController.class);

    private final KraAuthService kraAuthService;

    public EtimsTestController(KraAuthService kraAuthService) {
        this.kraAuthService = kraAuthService;
    }

    /**
     * Triggers the KRA OAuth2 client-credentials handshake and returns the
     * sanitised token details (never the raw secret/key, only what KRA sends back).
     *
     * @return 200 with token metadata on success, or a structured error body on failure.
     */
    @GetMapping("/auth")
    public ResponseEntity<?> testAuth() {
        log.info("Manual auth test triggered via GET /test/kra/auth");
        try {
            KraAuthResponse token = kraAuthService.fetchToken();
            return ResponseEntity.ok(Map.of(
                    "status",    "SUCCESS",
                    "tokenType", token.tokenType() != null ? token.tokenType() : "unknown",
                    "expiresIn", token.expiresIn(),
                    "tokenSnippet", obfuscate(token.accessToken())
            ));
        } catch (RestClientResponseException ex) {
            log.error("Auth test failed: {}", ex.getMessage());
            return ResponseEntity
                    .status(ex.getStatusCode())
                    .body(Map.of(
                            "status",  "KRA_ERROR",
                            "httpStatus", ex.getStatusCode().value(),
                            "kraMessage", ex.getResponseBodyAsString()
                    ));
        } catch (Exception ex) {
            log.error("Unexpected failure during auth test", ex);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status",  "INTERNAL_ERROR",
                            "message", ex.getMessage()
                    ));
        }
    }

    /**
     * Returns the first 8 characters of a token followed by ellipsis so you can
     * visually confirm a token arrived without logging the full JWT to the browser.
     */
    private String obfuscate(String token) {
        if (token == null || token.length() < 8) return "***";
        return token.substring(0, 8) + "...";
    }
}
