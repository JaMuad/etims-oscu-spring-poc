package com.muad.etims.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload for the /selectInitOsdcInfo endpoint to initialize the OSCU device
 * and retrieve the Communication Key (cmcKey).
 */
public record InitDeviceRequest(
        
        @JsonProperty("tin")
        String tin,
        
        @JsonProperty("bhfId")
        String bhfId,
        
        @JsonProperty("dvcSrlNo")
        String dvcSrlNo
) {}
