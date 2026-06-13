package com.example.travel_assistant.dto.external;

import java.util.List;

public record WikipediaSearchResponse(WikipediaQuery query) {

    public record WikipediaQuery(
            List<WikipediaSearchResult> search
    ) { }

    public record WikipediaSearchResult(
            Integer pageid,
            String title,
            String snippet
    ) { }
}
