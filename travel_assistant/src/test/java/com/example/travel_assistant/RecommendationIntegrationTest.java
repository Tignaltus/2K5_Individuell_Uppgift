package com.example.travel_assistant;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class RecommendationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final MockWebServer smhiServer = startMockServer();
    private static final MockWebServer wikipediaServer = startMockServer();

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("smhi.api.base-url", () -> smhiServer.url("/").toString());
        registry.add("wikipedia.api.base-url", () -> wikipediaServer.url("/").toString());
    }

    @AfterAll
    static void tearDown() throws IOException {
        smhiServer.shutdown();
        wikipediaServer.shutdown();
    }

    @Test
    void getRecommendations_whenExternalApisWork_returnsRecommendations() throws Exception {
        enqueueSmhiSuccessResponse(5);
        enqueueWikipediaSuccessResponse("Kulturen");

        MvcResult mvcResult = mockMvc.perform(get("/api/recommendations")
                        .param("location", "Lund"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Lund"))
                .andExpect(jsonPath("$.weather").value("Cloudy"))
                .andExpect(jsonPath("$.activityType").value("museum"))
                .andExpect(jsonPath("$.recommendations[0].name").value("Kulturen"))
                .andExpect(jsonPath("$.fallbackUsed").value(false));
    }

    @Test
    void getRecommendations_whenSmhiFails_usesWeatherFallback() throws Exception {
        enqueueSmhiServerErrorResponses();
        enqueueWikipediaSuccessResponse("Lunds stadspark");

        MvcResult mvcResult = mockMvc.perform(get("/api/recommendations")
                        .param("location", "Lund"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Lund"))
                .andExpect(jsonPath("$.weather").value("Clear"))
                .andExpect(jsonPath("$.activityType").value("park"))
                .andExpect(jsonPath("$.fallbackUsed").value(true));
    }

    @Test
    void getRecommendations_whenWikipediaFails_usesActivityFallback() throws Exception {
        enqueueSmhiSuccessResponse(1);
        enqueueWikipediaServerErrorResponses();

        MvcResult mvcResult = mockMvc.perform(get("/api/recommendations")
                        .param("location", "Lund"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location").value("Lund"))
                .andExpect(jsonPath("$.weather").value("Clear"))
                .andExpect(jsonPath("$.activityType").value("park"))
                .andExpect(jsonPath("$.recommendations[0].name", containsString("Explore central Lund")))
                .andExpect(jsonPath("$.fallbackUsed").value(true));
    }

    private static MockWebServer startMockServer() {
        MockWebServer server = new MockWebServer();

        try {
            server.start();
            return server;
        } catch (IOException e) {
            throw new RuntimeException("Could not start MockWebServer", e);
        }
    }

    private static void enqueueSmhiSuccessResponse(int symbolCode) {
        String body = """
                {
                  "timeSeries": [
                    {
                      "time": "2026-06-13T20:00:00Z",
                      "data": {
                        "air_temperature": 16.5,
                        "symbol_code": %d,
                        "precipitation_amount_mean": 0.0
                      }
                    }
                  ]
                }
                """.formatted(symbolCode);

        smhiServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(body));
    }

    private static void enqueueWikipediaSuccessResponse(String title) {
        String body = """
                {
                  "query": {
                    "search": [
                      {
                        "pageid": 123,
                        "title": "%s",
                        "snippet": "A recommended place to visit in Lund."
                      }
                    ]
                  }
                }
                """.formatted(title);

        wikipediaServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(body));
    }

    private static void enqueueSmhiServerErrorResponses() {
        smhiServer.enqueue(new MockResponse().setResponseCode(500));
        smhiServer.enqueue(new MockResponse().setResponseCode(500));
        smhiServer.enqueue(new MockResponse().setResponseCode(500));
    }

    private static void enqueueWikipediaServerErrorResponses() {
        wikipediaServer.enqueue(new MockResponse().setResponseCode(500));
        wikipediaServer.enqueue(new MockResponse().setResponseCode(500));
        wikipediaServer.enqueue(new MockResponse().setResponseCode(500));
    }
}