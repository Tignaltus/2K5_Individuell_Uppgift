package com.example.travel_assistant.dto;

import java.util.List;

public record RecommendationResponse(String location, String weather, String activityType,
                                     List<ActivityDTO> recommendations, boolean fallbackUsed) {
}
