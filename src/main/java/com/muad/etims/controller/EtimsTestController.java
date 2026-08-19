package com.muad.etims.controller;

import com.muad.etims.dto.response.InitDeviceResponse;
import com.muad.etims.service.KraInitializationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/test")
public class EtimsTestController {

    private final KraInitializationService kraInitializationService;

    public EtimsTestController(KraInitializationService kraInitializationService) {
        this.kraInitializationService = kraInitializationService;
    }

    /**
     * Endpoint to quickly test connecting to KRA and grabbing a token.
     */
    @GetMapping("/kra-auth")
    public ResponseEntity<?> testKraAuth() {
        log.info("Test Endpoint triggered: /api/v1/test/kra-auth");
        try {
            InitDeviceResponse response = kraInitializationService.initializeDevice();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Successfully reached KRA Initialization endpoint",
                    "data", response
            ));
        } catch (RestClientResponseException ex) {
            log.error("KRA HTTP Error", ex);
            return ResponseEntity.status(ex.getStatusCode()).body(Map.of(
                    "status", "error",
                    "message", ex.getMessage(),
                    "body", ex.getResponseBodyAsString()
            ));
        } catch (Exception ex) {
            log.error("KRA Auth Failure", ex);
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
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
