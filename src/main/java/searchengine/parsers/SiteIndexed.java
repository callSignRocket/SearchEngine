package searchengine.parsers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.statistics.StatisticsIndex;
import searchengine.dto.statistics.StatisticsLemma;
import searchengine.dto.statistics.StatisticsPage;
import searchengine.model.*;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;

@RequiredArgsConstructor
public class SiteIndexed implements Runnable {
    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final LemmaParser lemmaParser;
    private final IndexParser indexParser;
    private static final int coreCount = Runtime.getRuntime().availableProcessors();
    private final String url;
    private final SitesList sitesList;

    @Override
    public void run() {
        saveDateSite();
        try {
            List<StatisticsPage> statisticsPageList = getStatisticsPageList();
            saveToBase(statisticsPageList);
            getLemmasPage();
            indexingWords();
        } catch (InterruptedException e) {
            errorSite();
            Thread.currentThread().interrupt();
        }
    }

    private List<StatisticsPage> getStatisticsPageList() throws InterruptedException {
        if (!Thread.interrupted()) {
            String urlFormat = url + "/";
            List<StatisticsPage> statisticsPageVector = new ArrayList<>();
            List<String> urlList = new ArrayList<>();
            ForkJoinPool forkJoinPool = new ForkJoinPool(coreCount);
            List<StatisticsPage> pages = forkJoinPool.invoke(new UrlParser(urlFormat, statisticsPageVector, urlList));
            return new CopyOnWriteArrayList<>(pages);
        } else throw new InterruptedException();
    }

    private void getLemmasPage() {
        if (!Thread.interrupted()) {
            SiteEntity site = siteRepository.findByUrl(url);
            site.setStatusTime(LocalDateTime.now());
            lemmaParser.run(site);
            List<StatisticsLemma> statisticsLemmaList = lemmaParser.getLemmaDtoList();
            List<LemmaEntity> lemmaList = new ArrayList<>(statisticsLemmaList.size());
            for (StatisticsLemma statisticsLemma : statisticsLemmaList) {
                lemmaList.add(new LemmaEntity(statisticsLemma.lemma(), statisticsLemma.frequency(), site));
            }
            lemmaRepository.saveAll(lemmaList);
            lemmaRepository.flush();
        } else throw new RuntimeException();
    }

    private void saveToBase(List<StatisticsPage> pages) throws InterruptedException {
        if (!Thread.interrupted()) {
            SiteEntity site = siteRepository.findByUrl(url);
            List<PageEntity> pageList = new ArrayList<>(pages.size());
            for (StatisticsPage page : pages) {
                int first = page.url().indexOf(url) + url.length();
                String format = page.url().substring(first);
                pageList.add(new PageEntity(site, format, page.code(), page.content()));
            }
            pageRepository.saveAll(pageList);
            pageRepository.flush();
        } else throw new InterruptedException();
    }

    private void indexingWords() throws InterruptedException {
        if (!Thread.interrupted()) {
            SiteEntity site = siteRepository.findByUrl(url);
            indexParser.run(site);
            List<StatisticsIndex> statisticsIndexList = indexParser.getIndexList();
            List<IndexEntity> indexList = new ArrayList<>(statisticsIndexList.size());
            site.setStatusTime(LocalDateTime.now());
            for (StatisticsIndex statisticsIndex : statisticsIndexList) {
                PageEntity page = pageRepository.getReferenceById(statisticsIndex.pageId());
                LemmaEntity lemma = lemmaRepository.getReferenceById(statisticsIndex.lemmaId());
                indexList.add(new IndexEntity(page, lemma, statisticsIndex.rank()));
            }
            indexRepository.saveAll(indexList);
            indexRepository.flush();
            site.setStatusTime(LocalDateTime.now());
            site.setStatus(Status.INDEXED);
            siteRepository.save(site);
        } else throw new InterruptedException();
    }

    private void saveDateSite() {
        SiteEntity site = new SiteEntity();
        site.setUrl(url);
        site.setName(getName());
        site.setStatus(Status.INDEXING);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.flush();
        siteRepository.save(site);
    }

    private String getName() {
        List<Site> siteList = sitesList.getSites();
        for (Site site : siteList) {
            if (site.getUrl().equals(url)) {
                return site.getName();
            }
        }
        return "";
    }

    private void errorSite() {
        SiteEntity site = new SiteEntity();
        site.setLastError("Индексация прервана");
        site.setStatus(Status.FAILED);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.save(site);
    }
}
