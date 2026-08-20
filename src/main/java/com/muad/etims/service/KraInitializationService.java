package com.muad.etims.service;

import com.muad.etims.dto.request.InitDeviceRequest;
import com.muad.etims.dto.response.InitDeviceResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Handles the device initialization with the KRA eTIMS API.
 * Uses the /selectInitOsdcInfo endpoint to register the OSCU device
 * and fetch the Communication Key (cmcKey).
 */
@Service
public class KraInitializationService {

    private static final Logger log = LoggerFactory.getLogger(KraInitializationService.class);
    private static final String INIT_PATH = "/selectInitOsdcInfo";

    private final RestClient restClient;
    private final String pin;
    private final String branchId;
    private final String deviceSerial;

    public KraInitializationService(
            RestClient restClient,
            @Value("${kra.api.pin}") String pin,
            @Value("${kra.api.branch-id:00}") String branchId,
            @Value("${kra.api.device-serial}") String deviceSerial
    ) {
        this.restClient = restClient;
        this.pin = pin;
        this.branchId = branchId;
        this.deviceSerial = deviceSerial;
    }

    /**
     * Initializes the OSCU device with KRA to obtain the cmcKey.
     *
     * @return {@link InitDeviceResponse} containing the cmcKey.
     */
    public InitDeviceResponse initializeDevice() {
        InitDeviceRequest requestPayload = new InitDeviceRequest(pin, branchId, deviceSerial);
        log.info("Requesting KRA device initialization at {} for PIN: {}, Serial: {}", INIT_PATH, pin, deviceSerial);

        try {
            InitDeviceResponse response = restClient.post()
                    .uri(INIT_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestPayload)
                    .retrieve()
                    .body(InitDeviceResponse.class);

            if (response != null && "000".equals(response.resultCd())) {
                log.info("Successfully initialized device with KRA! Taxpayer: {}", 
                         response.data() != null && response.data().info() != null ? response.data().info().taxprNm() : "Unknown");
            } else {
                log.error("Failed to initialize device: {} - {}", 
                          response != null ? response.resultCd() : "NULL", 
                          response != null ? response.resultMsg() : "No response body");
            }

            return response;
        } catch (Exception ex) {
            log.error("Initialization request failed completely", ex);
            throw ex;
        }
    }
}
