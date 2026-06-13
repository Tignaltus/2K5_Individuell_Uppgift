package com.example.travel_assistant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean(name = "smhiWebClient")
    public WebClient smhiWebClient(WebClient.Builder builder, @Value("${smhi.api.base-url}") String baseUrl) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }

    @Bean(name = "wikipediaWebClient")
    public WebClient wikipediaWebClient(@Value("${wikipedia.api.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.USER_AGENT, "travel-assistant-school-project/1.0")
                .build();
    }
}
