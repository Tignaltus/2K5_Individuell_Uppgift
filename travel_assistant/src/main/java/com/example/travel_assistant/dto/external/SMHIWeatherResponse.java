package com.example.travel_assistant.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SMHIWeatherResponse(List<SMHITimeSeries> timeSeries) {

    public record SMHITimeSeries(String time, SMHIWeatherData data) {
    }

    public record SMHIWeatherData(
            @JsonProperty("air_temperature")
            Double airTemperature,

            @JsonProperty("symbol_code")
            Integer symbolCode,

            @JsonProperty("precipitation_amount_mean")
            Double precipitationAmountMean
    ) { }
}
