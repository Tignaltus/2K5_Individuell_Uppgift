package com.example.travel_assistant.dto;

public record WeatherDTO(String location, String condition, String description, Double temperature, boolean fallbackUsed) {

}
