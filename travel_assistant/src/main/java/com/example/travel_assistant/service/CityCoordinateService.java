package com.example.travel_assistant.service;

import com.example.travel_assistant.dto.CityCoordinate;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CityCoordinateService {

    public CityCoordinate getCoordinates(String location) {
        String normalizedLocation = location.trim().toLowerCase();

        return switch (normalizedLocation) {
            case "lund" -> new CityCoordinate("Lund", 55.7047, 13.1910);
            case "malmö", "malmo" -> new CityCoordinate("Malmö", 55.6050, 13.0038);
            case "stockholm" -> new CityCoordinate("Stockholm", 59.3293, 18.0686);
            case "göteborg", "goteborg", "gothenburg" -> new CityCoordinate("Göteborg", 57.7089, 11.9746);
            case "helsingborg" -> new CityCoordinate("Helsingborg", 56.0465, 12.6945);
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unknown location: " + location + ". Try Lund, Malmö, Stockholm, Göteborg or Helsingborg."
            );
        };
    }
}
