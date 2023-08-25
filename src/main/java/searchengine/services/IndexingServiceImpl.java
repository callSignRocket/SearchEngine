package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.parsers.IndexParser;
import searchengine.parsers.LemmaParser;
import searchengine.parsers.SiteIndexed;
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
    public void indexingAll() {
        if (isIndexingActive()) {
            log.debug("Индексация уже запущена");
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
    }

    @Override
    public boolean stopIndexing() {
        if (isIndexingActive()) {
            log.info("Индексация была остановлена");
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
            if (site.getUrl().equals(url)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void removeSiteFromIndex(String url) {
        IndexingService indexingService = new IndexingServiceImpl(pageRepository, siteRepository, lemmaRepository,
                indexRepository, lemmaParser, indexParser, sitesList);
        if (urlCheck(url)) {
            siteRepository.deleteByUrl(url);
            indexingService.urlIndexing(url);
        }
    }
}
