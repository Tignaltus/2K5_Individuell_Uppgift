package com.example.travel_assistant.client;

import com.example.travel_assistant.dto.WeatherDTO;
import com.example.travel_assistant.dto.external.SMHIWeatherResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

@Service
public class SMHIWeatherClient {

    private final WebClient smhiWebClient;

    public SMHIWeatherClient(@Qualifier("smhiWebClient") WebClient smhiWebClient) {
        this.smhiWebClient = smhiWebClient;
    }

    public Mono<WeatherDTO> getCurrentWeather(String location, double latitude, double longitude) {
        return smhiWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/category/snow1g/version/1/geotype/point/lon/{lon}/lat/{lat}/data.json")
                        .queryParam("timeseries", 1)
                        .queryParam("parameters", "air_temperature,symbol_code,precipitation_amount_mean")
                        .build(longitude, latitude))
                .retrieve()
                .bodyToMono(SMHIWeatherResponse.class)
                .timeout(Duration.ofSeconds(3))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500)))
                .map(response -> mapToWeatherDTO(location, response))
                .onErrorResume(error -> Mono.just(createFallbackWeather(location)));
    }

    private WeatherDTO mapToWeatherDTO(String location, SMHIWeatherResponse response) {
        if (response.timeSeries() == null || response.timeSeries().isEmpty()) {
            return createFallbackWeather(location);
        }

        SMHIWeatherResponse.SMHIWeatherData data = response.timeSeries().get(0).data();

        Integer symbolCode = data.symbolCode();

        return new WeatherDTO(
                location,
                mapSymbolCodeToCondition(symbolCode),
                mapSymbolCodeToDescription(symbolCode),
                data.airTemperature(),
                false
        );
    }

    private WeatherDTO createFallbackWeather(String location) {
        return new WeatherDTO(
                location,
                "Clear",
                "Fallback weather used because SMHI could not be reached.",
                15.0,
                true
        );
    }

    private String mapSymbolCodeToCondition(Integer symbolCode) {
        if (symbolCode == null) {
            return "Unknown";
        }

        return switch (symbolCode) {
            case 1, 2 -> "Clear";
            case 3, 4, 5, 6 -> "Cloudy";
            case 7 -> "Fog";
            case 8, 9, 10, 18, 19, 20 -> "Rain";
            case 11, 21 -> "Thunder";
            case 12, 13, 14, 22, 23, 24 -> "Sleet";
            case 15, 16, 17, 25, 26, 27 -> "Snow";
            default -> "Unknown";
        };
    }

    private String mapSymbolCodeToDescription(Integer symbolCode) {
        if (symbolCode == null) {
            return "No weather symbol available";
        }

        return switch (symbolCode) {
            case 1 -> "Clear sky";
            case 2 -> "Nearly clear sky";
            case 3 -> "Variable cloudiness";
            case 4 -> "Halfclear sky";
            case 5 -> "Cloudy sky";
            case 6 -> "Overcast";
            case 7 -> "Fog";
            case 8 -> "Light rain showers";
            case 9 -> "Moderate rain showers";
            case 10 -> "Heavy rain showers";
            case 11 -> "Thunderstorm";
            case 12 -> "Light sleet showers";
            case 13 -> "Moderate sleet showers";
            case 14 -> "Heavy sleet showers";
            case 15 -> "Light snow showers";
            case 16 -> "Moderate snow showers";
            case 17 -> "Heavy snow showers";
            case 18 -> "Light rain";
            case 19 -> "Moderate rain";
            case 20 -> "Heavy rain";
            case 21 -> "Thunder";
            case 22 -> "Light sleet";
            case 23 -> "Moderate sleet";
            case 24 -> "Heavy sleet";
            case 25 -> "Light snowfall";
            case 26 -> "Moderate snowfall";
            case 27 -> "Heavy snowfall";
            default -> "Unknown weather condition";
        };
    }
}