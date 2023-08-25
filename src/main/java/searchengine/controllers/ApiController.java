package searchengine.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.statistics.TrueResponse;
import searchengine.dto.statistics.FalseResponse;
import searchengine.dto.statistics.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.StatisticsSearch;
import searchengine.repositories.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import javax.annotation.PostConstruct;
import java.util.List;

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
    public ResponseEntity<Object> startIndexing(@RequestParam(name = "site") String siteUrl) {
        if (siteUrl.isEmpty()) {
            return new ResponseEntity<>(new FalseResponse(false, "Не указан сайт для индексации"), HttpStatus.BAD_REQUEST);
        } else {
            if (siteRepository.findByUrl(siteUrl) == null) {
                return new ResponseEntity<>(new FalseResponse(false, "Сайт не найден в базе"), HttpStatus.BAD_REQUEST);
            }
            indexingService.removeSiteFromIndex(siteUrl);
            indexingService.urlIndexing(siteUrl);
            return new ResponseEntity<>(new TrueResponse(true), HttpStatus.OK);
        }
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing() {
        if (indexingService.stopIndexing()) {
            return new ResponseEntity<>(new TrueResponse(true), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new FalseResponse(false, "Индексация не была запущена"), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Object> search(@RequestParam(name = "query", required = false, defaultValue = "") String query,
                                         @RequestParam(name = "site", required = false, defaultValue = "") String site,
                                         @RequestParam(name = "offset", required = false, defaultValue = "0") int offset,
                                         @RequestParam(name = "limit", required = false, defaultValue = "20") int limit) {
        if (query.isEmpty()) {
            return new ResponseEntity<>(new FalseResponse(false, "Задан пустой поисковой запрос"), HttpStatus.BAD_REQUEST);
        } else {
            List<StatisticsSearch> searchData;
            if (!site.isEmpty()) {
                if (siteRepository.findByUrl(site) == null) {
                    return new ResponseEntity<>(new FalseResponse(false, "Страница не найдена"), HttpStatus.BAD_REQUEST);
                } else {
                    searchData = searchService.siteSearch(query, site, offset, limit);
                }
            } else {
                searchData = searchService.allSiteSearch(query, offset, limit);
            }
            return new ResponseEntity<>(new SearchResponse(true, searchData.size(), searchData), HttpStatus.OK);
        }
    }

    @PostMapping("/indexPage")
    public ResponseEntity<Object> indexPage(@RequestParam(name = "url") String url) {
        if (url.isEmpty()) {
            return new ResponseEntity<>(new FalseResponse(false, "Страница не указана"), HttpStatus.BAD_REQUEST);
        } else {
            if (indexingService.urlIndexing(url)) {
                return new ResponseEntity<>(new TrueResponse(true), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new FalseResponse(false, "Введите страницу из файла конфигурации"),
                        HttpStatus.BAD_REQUEST);
            }
        }
    }

    @PostConstruct
    public void initialize() {
        siteRepository.deleteAll();
        indexingService.indexingAll();
    }
}
