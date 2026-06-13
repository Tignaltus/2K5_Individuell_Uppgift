package com.example.travel_assistant.dto;

import java.util.List;

public record ActivityResultDTO(
        List<ActivityDTO> activities,
        boolean fallbackUsed
) {
}