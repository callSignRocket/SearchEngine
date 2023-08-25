package searchengine.dto.statistics;

public record StatisticsSearch(String site, String siteName, String uri,
                               String title, String snippet, Float relevance) {
}

