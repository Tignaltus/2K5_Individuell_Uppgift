package com.example.travel_assistant.client;

import com.example.travel_assistant.dto.ActivityDTO;
import com.example.travel_assistant.dto.ActivityResultDTO;
import com.example.travel_assistant.dto.external.WikipediaSearchResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.HtmlUtils;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Service
public class ActivityClient {

    private final WebClient wikipediaWebClient;

    public ActivityClient(@Qualifier("wikipediaWebClient") WebClient wikipediaWebClient) {
        this.wikipediaWebClient = wikipediaWebClient;
    }

    public Mono<ActivityResultDTO> getActivities(String location, String activityType) {
        String searchTerm = createSearchTerm(location, activityType);

        return wikipediaWebClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api.php")
                        .queryParam("action", "query")
                        .queryParam("list", "search")
                        .queryParam("srsearch", searchTerm)
                        .queryParam("srlimit", 5)
                        .queryParam("format", "json")
                        .build())
                .retrieve()
                .bodyToMono(WikipediaSearchResponse.class)
                .timeout(Duration.ofSeconds(3))
                .retryWhen(Retry.backoff(2, Duration.ofMillis(500)))
                .map(response -> mapToActivityResult(response, location, activityType))
                .onErrorResume(error -> Mono.just(createFallbackActivities(location, activityType)));
    }

    private String createSearchTerm(String location, String activityType) {
        return switch (activityType.toLowerCase()) {
            case "museum" -> "museum " + location;
            case "park" -> "park " + location;
            case "cafe" -> "café " + location;
            case "indoor activity" -> "museum " + location;
            case "tourist attraction" -> "sevärdheter " + location;
            default -> activityType + " " + location;
        };
    }

    private ActivityResultDTO mapToActivityResult(
            WikipediaSearchResponse response,
            String location,
            String activityType
    ) {
        if (response.query() == null ||
                response.query().search() == null ||
                response.query().search().isEmpty()) {
            return createFallbackActivities(location, activityType);
        }

        List<ActivityDTO> activities = response.query().search().stream()
                .map(result -> new ActivityDTO(
                        result.title(),
                        activityType,
                        cleanSnippet(result.snippet())
                ))
                .toList();

        return new ActivityResultDTO(activities, false);
    }

    private ActivityResultDTO createFallbackActivities(String location, String activityType) {
        List<ActivityDTO> fallbackActivities = List.of(
                new ActivityDTO(
                        "Explore central " + location,
                        activityType,
                        "Fallback activity: take a walk and explore the city center."
                ),
                new ActivityDTO(
                        "Visit a local café in " + location,
                        "cafe",
                        "Fallback activity: choose a nearby café if no external recommendations are available."
                ),
                new ActivityDTO(
                        "Visit a local museum or landmark",
                        "museum",
                        "Fallback activity: indoor recommendation used when the activity API is unavailable."
                )
        );

        return new ActivityResultDTO(fallbackActivities, true);
    }

    private String cleanSnippet(String snippet) {
        if (snippet == null || snippet.isBlank()) {
            return "No description available.";
        }

        String withoutHtmlTags = snippet.replaceAll("<[^>]*>", "");
        return HtmlUtils.htmlUnescape(withoutHtmlTags);
    }
}