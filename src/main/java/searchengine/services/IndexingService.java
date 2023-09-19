package searchengine.services;

import org.springframework.http.ResponseEntity;

public interface IndexingService {

    boolean urlIndexing(String url);
    boolean indexingAll();
    boolean stopIndexing();

    ResponseEntity<Object> indexingAllSites(boolean indexingAll);
    ResponseEntity<Object> indexingStop(boolean stopIndexing);
    ResponseEntity<Object> indexingSite(String url);
}
