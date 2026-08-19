package com.muad.etims.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps the JSON body returned by the KRA eTIMS /selectInitOsdcInfo endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InitDeviceResponse(
        @JsonProperty("resultCd")
        String resultCd,
        
        @JsonProperty("resultMsg")
        String resultMsg,
        
        @JsonProperty("resultDt")
        String resultDt,
        
        @JsonProperty("data")
        InitDeviceData data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InitDeviceData(
            @JsonProperty("info")
            InitDeviceInfo info
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InitDeviceInfo(
            @JsonProperty("tin")
            String tin,

            @JsonProperty("taxprNm")
            String taxprNm,

            @JsonProperty("cmcKey")
            String cmcKey
    ) {}
}
