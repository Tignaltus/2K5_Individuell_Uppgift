# 2K5_Individuell_Uppgift - Jonathan Isaksson

## Beskrivning

Applikationen fungerar som en smart rese-assistent som ger rekommendationer baserat på det aktuella vädret för en vald stad.

Användaren skickar in en stad via en REST-endpoint. Systemet hämtar väderdata från SMHI och använder vädret för att avgöra vilken typ av aktivitet som passar bäst. Därefter anropas Wikipedia/MediaWiki API för att hämta rekommendationer kopplade till staden och aktivitetstypen.

Projektet fokuserar på API-integrationer, asynkrona anrop med WebClient och feltolerans genom fallback, timeout och retry.

---

## Tekniker

Projektet använder:

- Java 21
- Spring Boot 4.1.0
- Spring Web
- Spring WebClient
- Maven
- DTO:er/Records
- Reactor Mono
- MockMvc
- MockWebServer
- JUnit 5

---

## Externa API:er

Projektet använder två externa API:er:

### 1. SMHI Open Data

SMHI används som väder-API. Eftersom SMHI:s väder-API använder koordinater har projektet en `CityCoordinateService` som översätter ett antal städer till latitud och longitud.

Exempel på stödda städer:

- Lund
- Malmö
- Stockholm
- Göteborg
- Helsingborg

### 2. Wikipedia/MediaWiki API

Wikipedia/MediaWiki används som aktivitets-/rekommendations-API. Systemet söker efter aktiviteter baserat på stad och väderbaserad aktivitetstyp.

Exempel:

- Regnigt väder → museum
- Klart väder → park
- Snö → café
- Molnigt väder → museum eller inomhusaktivitet

---

## API-endpoint

Applikationen exponerar följande endpoint:

```http
GET /api/recommendations?location={city}
```

Exempel:

```http
GET http://localhost:8080/api/recommendations?location=Lund
```

---

## Exempel på JSON-svar

```json
{
  "location": "Lund",
  "weather": "Cloudy",
  "activityType": "museum",
  "recommendations": [
    {
      "name": "Kulturen",
      "category": "museum",
      "description": "A recommended place to visit in Lund."
    }
  ],
  "fallbackUsed": false
}
```

---

## Projektstruktur

Projektet är uppdelat i flera lager:

```text
com.example.travel_assistant
│
├── client
│   ├── SMHIWeatherClient
│   └── ActivityClient
│
├── config
│   └── WebClientConfig
│
├── controller
│   └── RecommendationController
│
├── dto
│   ├── ActivityDTO
│   ├── ActivityResultDTO
│   ├── CityCoordinate
│   ├── RecommendationResponse
│   └── WeatherDTO
│
├── dto.external
│   ├── SmhiWeatherResponse
│   └── WikipediaSearchResponse
│
├── service
│   ├── CityCoordinateService
│   └── RecommendationService
│
└── TravelAssistantApplication
```

---

## Flöde i applikationen

1. Klienten skickar en request till:

```http
GET /api/recommendations?location=Lund
```

2. `RecommendationController` tar emot requesten.

3. `RecommendationService` hämtar koordinater för staden via `CityCoordinateService`.

4. `SMHIWeatherClient` anropar SMHI med WebClient.

5. Vädret tolkas och översätts till en aktivitetstyp.

6. `ActivityClient` söker efter rekommendationer via Wikipedia/MediaWiki API.

7. Systemet returnerar ett samlat JSON-svar till klienten.

---

## Feltolerans och resiliens

Projektet hanterar fel från externa API:er så att applikationen inte kraschar om en extern tjänst slutar svara.

Följande mönster används:

### Timeout

Externa API-anrop avbryts om de tar för lång tid.

### Retry

Vid tillfälliga fel försöker systemet igen innan fallback används.

### Fallback

Om SMHI-anropet misslyckas används standardväder:

```text
Clear
```

Om aktivitets-API:t misslyckas returneras en hårdkodad lista med reservaktiviteter.

Exempel på fallback-aktivitet:

```text
Explore central Lund
```

I JSON-svaret visas även om fallback har använts:

```json
"fallbackUsed": true
```

---

## Testning

Projektet innehåller integrationstester med:

- Spring Boot Test
- MockMvc
- MockWebServer

MockWebServer används för att simulera externa API:er utan att testerna är beroende av riktiga externa tjänster.

Testerna verifierar att:

1. Endpointen fungerar när båda externa API:erna svarar korrekt.
2. Weather fallback används när SMHI returnerar serverfel.
3. Activity fallback används när Wikipedia/MediaWiki API returnerar serverfel.
4. Applikationen returnerar ett fungerande JSON-svar även när externa API:er misslyckas.

Eftersom endpointen returnerar `Mono` körs testerna med async-hantering via `asyncDispatch`.

---

## Köra projektet

Starta projektet från IntelliJ eller via terminal:

```bash
mvn spring-boot:run
```

När applikationen körs kan endpointen testas i webbläsare eller Postman:

```http
http://localhost:8080/api/recommendations?location=Lund
```

---

## Köra tester

Kör testerna med:

```bash
mvn test
```

Alla integrationstester ska passera.

---

## Konfiguration

I `application.properties` finns bas-URL:er för de externa API:erna:

```properties
smhi.api.base-url=https://opendata-download-metfcst.smhi.se
wikipedia.api.base-url=https://sv.wikipedia.org/w
```

Inga API-nycklar behövs för de API:er som används i projektet.

---

## Manuell fallback-testning

För att manuellt testa fallback kan man tillfälligt ändra en API-url i `application.properties`.

Exempel:

```properties
smhi.api.base-url=https://fake-smhi-url.example
```

När applikationen startas om och endpointen anropas ska svaret fortfarande returneras, men med:

```json
"fallbackUsed": true
```

Efter testet ska URL:en ändras tillbaka:

```properties
smhi.api.base-url=https://opendata-download-metfcst.smhi.se
```

---

## Avgränsningar

Projektet använder en enkel hårdkodad lista med städer och koordinater. En mer avancerad version hade kunnat använda ett separat geocoding-API för att stödja fler städer dynamiskt.

Projektet använder fallback, timeout och retry. Circuit Breaker är inte implementerat i denna version.

---

## Sammanfattning

Projektet uppfyller målet att bygga en Spring Boot-backend som integrerar med externa API:er via WebClient och hanterar externa fel på ett feltolerant sätt.

Applikationen har:

- En egen REST-endpoint
- Två externa API-integrationer
- Asynkrona WebClient-anrop
- DTO:er för intern och extern data
- Väderbaserad rekommendationslogik
- Timeout
- Retry
- Fallback
- Integrationstester som verifierar felhantering
