package com.example.travel_assistant.service;

import com.example.travel_assistant.client.ActivityClient;
import com.example.travel_assistant.client.SMHIWeatherClient;
import com.example.travel_assistant.dto.CityCoordinate;
import com.example.travel_assistant.dto.RecommendationResponse;
import com.example.travel_assistant.dto.WeatherDTO;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class RecommendationService {

    private final SMHIWeatherClient smhiWeatherClient;
    private final CityCoordinateService cityCoordinateService;
    private final ActivityClient activityClient;

    public RecommendationService(
            SMHIWeatherClient smhiWeatherClient,
            CityCoordinateService cityCoordinateService,
            ActivityClient activityClient
    ) {
        this.smhiWeatherClient = smhiWeatherClient;
        this.cityCoordinateService = cityCoordinateService;
        this.activityClient = activityClient;
    }

    public Mono<RecommendationResponse> getRecommendations(String location) {
        CityCoordinate coordinates = cityCoordinateService.getCoordinates(location);

        return smhiWeatherClient
                .getCurrentWeather(
                        coordinates.cityName(),
                        coordinates.latitude(),
                        coordinates.longitude()
                )
                .flatMap(weather -> {
                    String activityType = chooseActivityType(weather);

                    return activityClient
                            .getActivities(coordinates.cityName(), activityType)
                            .map(activityResult -> new RecommendationResponse(
                                    coordinates.cityName(),
                                    weather.condition(),
                                    activityType,
                                    activityResult.activities(),
                                    weather.fallbackUsed() || activityResult.fallbackUsed()
                            ));
                });
    }

    private String chooseActivityType(WeatherDTO weather) {
        String condition = weather.condition().toLowerCase();

        if (condition.contains("rain") || condition.contains("thunder") || condition.contains("sleet")) {
            return "museum";
        }

        if (condition.contains("clear")) {
            return "park";
        }

        if (condition.contains("snow")) {
            return "cafe";
        }

        if (condition.contains("cloud") || condition.contains("fog")) {
            return "museum";
        }

        return "tourist attraction";
    }
}