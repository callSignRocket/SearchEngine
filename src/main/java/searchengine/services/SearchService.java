package searchengine.services;

import org.springframework.http.ResponseEntity;
import searchengine.dto.statistics.SearchResponse;
import searchengine.dto.statistics.StatisticsSearch;

import java.util.List;

public interface SearchService {
    SearchResponse allSiteSearch(String text, int offset, int limit);
    SearchResponse siteSearch(String searchText, String url, int offset, int limit);
    ResponseEntity<Object> search(String searchText, String url, int offset, int limit);
}
