package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.FalseResponse;
import searchengine.dto.statistics.TrueResponse;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.utils.parsers.IndexParser;
import searchengine.utils.parsers.LemmaParser;
import searchengine.utils.parsers.SiteIndexed;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexingServiceImpl implements IndexingService{

    private static final int processorCoreCount = Runtime.getRuntime().availableProcessors();
    private ExecutorService executorService;
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaParser lemmaParser;
    private final IndexParser indexParser;
    private final SitesList sitesList;

    @Override
    public boolean urlIndexing(String url) {
        if (urlCheck(url)) {
            log.info("Переиндексация сайта - " + url);
            executorService = Executors.newFixedThreadPool(processorCoreCount);
            executorService.submit(new SiteIndexed(pageRepository, siteRepository, lemmaRepository,
                    indexRepository, lemmaParser, indexParser, url, sitesList));
            executorService.shutdown();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean indexingAll() {
        if (isIndexingActive()) {
            log.debug("Индексация уже запущена");
            return false;
        } else {
            List<Site> siteList = sitesList.getSites();
            executorService = Executors.newFixedThreadPool(processorCoreCount);
            for (Site site : siteList) {
                String url = site.getUrl();
                SiteEntity siteEntity = new SiteEntity();
                siteEntity.setName(site.getName());
                log.info("Парсинг сайта: " + site.getName());
                executorService.submit(new SiteIndexed(pageRepository, siteRepository, lemmaRepository, indexRepository,
                        lemmaParser, indexParser, url, sitesList));
            }
            executorService.shutdown();
        }
        return true;
    }

    @Override
    public boolean stopIndexing() {
        if (isIndexingActive()) {
            log.info("Индексация остановлена пользователем");
            executorService.shutdownNow();
            return true;
        } else {
            log.info("Индексация не была запущена");
            return false;
        }
    }

    private boolean isIndexingActive() {
        siteRepository.flush();
        Iterable<SiteEntity> siteList = siteRepository.findAll();
        for (SiteEntity site : siteList) {
            if (site.getStatus() == Status.INDEXING) {
                return true;
            }
        }
        return false;
    }

    private boolean urlCheck(String url) {
        List<Site> urlList = sitesList.getSites();
        for (Site site : urlList) {
            if (url.startsWith(site.getUrl())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ResponseEntity<Object> indexingAllSites(boolean indexingAll) {
        if (indexingAll) {
            return new ResponseEntity<>(new TrueResponse(true), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new FalseResponse(false, "Индексация не запущена"), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public ResponseEntity<Object> indexingStop(boolean stopIndexing) {
        if (stopIndexing()) {
            return new ResponseEntity<>(new TrueResponse(true), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(new FalseResponse(false, "Индексация не была запущена"), HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    public ResponseEntity<Object> indexingSite(String url) {
        if (url.isEmpty()) {
            return new ResponseEntity<>(new FalseResponse(false, "Страница не указана"), HttpStatus.BAD_REQUEST);
        } else {
            if (urlIndexing(url)) {
                return new ResponseEntity<>(new TrueResponse(true), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new FalseResponse(false, "Введите страницу из файла конфигурации"),
                        HttpStatus.BAD_REQUEST);
            }
        }
    }
}
