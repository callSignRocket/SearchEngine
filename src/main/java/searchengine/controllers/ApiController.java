package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SiteRepository siteRepository;
    private final SearchService searchService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService, SiteRepository siteRepository, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.siteRepository = siteRepository;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<Object> startIndexing() {
        ResponseEntity<Object> response = indexingService.indexingAllSites(indexingService.indexingAll());
        return response;
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing() {
        ResponseEntity<Object> response = indexingService.indexingStop(indexingService.stopIndexing());
        return response;
    }


    @GetMapping("/search")
    public ResponseEntity<Object> search(@RequestParam(name = "query", required = false, defaultValue = "") String query,
                                         @RequestParam(name = "site", required = false, defaultValue = "") String site,
                                         @RequestParam(name = "offset", defaultValue = "0") int offset,
                                         @RequestParam(name = "limit", defaultValue = "20") int limit) {

        ResponseEntity<Object> response = searchService.search(query, site, offset, limit);
        return response;
    }


    @PostMapping("/indexPage")
    public ResponseEntity<Object> indexPage(@RequestParam(name = "url") String url) {
        ResponseEntity<Object> response = indexingService.indexingSite(url);
        return response;
    }
}
