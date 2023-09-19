package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.FalseResponse;
import searchengine.dto.statistics.SearchResponse;
import searchengine.dto.statistics.StatisticsSearch;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.morphology.Morphology;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;
import searchengine.utils.CleanHtmlCode;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService{
    private final Morphology morphology;
    private final LemmaRepository lemmaRepository;
    private final PageRepository pageRepository;
    private final IndexRepository indexRepository;
    private final SiteRepository siteRepository;

    @Override
    public SearchResponse allSiteSearch(String searchText, int offset, int limit) {
        log.info("Результат поиска: " + searchText);
        List<SiteEntity> siteList = siteRepository.findAll();
        List<StatisticsSearch> result;
        List<String> textLemmaList = getLemmaFromSearchText(searchText);
        List<LemmaEntity> foundLemmaList = getLemmasFromSiteList(siteList, textLemmaList);

        if (foundLemmaList.isEmpty()) {
            result = getSearchDtoList(foundLemmaList, textLemmaList, offset, limit);
            return new SearchResponse(true, result.size(), result);
        }

        List<StatisticsSearch> searchData = getSearchDtoList(foundLemmaList, textLemmaList, offset, limit);
        SearchResponse search = new SearchResponse(true, searchData.size(), searchData);
        log.info("Поиск завершен.");
        return search;
    }

    private List<LemmaEntity> getLemmasFromSiteList(List<SiteEntity> siteList, List<String> textLemmaList) {
        List<LemmaEntity> foundLemmaList = new ArrayList<>();
        for (SiteEntity site : siteList) {
            foundLemmaList.addAll(getLemmaListFromSite(textLemmaList, site));
        }
        return foundLemmaList;
    }

    @Override
    public SearchResponse siteSearch(String searchText, String url, int offset, int limit) {
        log.info("Поиск: " + "«" + searchText + "»" + " на странице: " + url);
        SiteEntity site = siteRepository.findByUrl(url);
        List<String> textLemmaList = getLemmaFromSearchText(searchText);
        List<LemmaEntity> foundLemmaList = getLemmaListFromSite(textLemmaList, site);
        log.info("Поиск завершен.");
        List<StatisticsSearch> data = getSearchDtoList(foundLemmaList, textLemmaList, offset, limit);
        return new SearchResponse(true, data.size(), data);
    }

    private List<String> getLemmaFromSearchText(String searchText) {
        String[] words = searchText.toLowerCase(Locale.ROOT).split(" ");
        List<String> lemmaList = new ArrayList<>();
        for (String lemma : words) {
            List<String> list = morphology.getLemma(lemma);
            lemmaList.addAll(list);
        }
        return lemmaList;
    }

    private List<LemmaEntity> getLemmaListFromSite(List<String> lemmas, SiteEntity site) {
        lemmaRepository.flush();
        List<LemmaEntity> lemmaList = new ArrayList<>();
        for (String lemma : lemmas) {
            lemmaList.addAll(lemmaRepository.findByLemmaAndSite(lemma, site));
        }
        lemmaList.sort(Comparator.comparingInt(LemmaEntity::getFrequency));
        return lemmaList;
    }

    private List<StatisticsSearch> getSearchData(Hashtable<PageEntity, Float> pageList, List<String> textLemmaList) {
        List<StatisticsSearch> result = new ArrayList<>();

        for (PageEntity page : pageList.keySet()) {
            String uri = page.getPath();
            String content = page.getContent();
            SiteEntity siteEntity = page.getSite();
            String site = siteEntity.getUrl();
            String siteName = siteEntity.getName();
            Float absRelevance = pageList.get(page);

            StringBuilder clearContent = new StringBuilder();
            String title = CleanHtmlCode.clear(content, "title");
            String body = CleanHtmlCode.clear(content, "body");
            clearContent.append(title).append(" ").append(body);
            String snippet = getSnippet(clearContent.toString(), textLemmaList);
            result.add(new StatisticsSearch(site, siteName, uri, title, snippet, absRelevance));
        }
        return result;
    }

    private String getSnippet(String content, List<String> lemmaList) {
        List<Integer> lemmaIndex = new ArrayList<>();
        StringBuilder result = new StringBuilder();
        for (String lemma : lemmaList) {
            lemmaIndex.addAll(morphology.findLemmaIndexInText(content, lemma));
        }
        Collections.sort(lemmaIndex);
        List<String> wordsList = getWordsFromContent(content, lemmaIndex);
        for (int i = 0; i < wordsList.size(); i++) {
            result.append(wordsList.get(i)).append("... ");
            if (i > 3) {
                break;
            }
        }
        return result.toString();
    }

    private List<String> getWordsFromContent(String content, List<Integer> lemmaIndex) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < lemmaIndex.size(); i++) {
            int start = lemmaIndex.get(i);
            int end = content.indexOf(" ", start);
            int nextIndex = i + 1;
            while (nextIndex < lemmaIndex.size()
                    && lemmaIndex.get(nextIndex) - end > 0
                    && lemmaIndex.get(nextIndex) - end < 5) {
                end = content.indexOf(" ", lemmaIndex.get(nextIndex));
                nextIndex++;
            }
            i = nextIndex - 1;
            String text = getWordsFromIndex(start, end, content);
            result.add(text);
        }
        result.sort(Comparator.comparingInt(String::length).reversed());
        return result;
    }

    private String getWordsFromIndex(int start, int end, String content) {
        String word = content.substring(start, end);
        int prevIndex;
        int lastIndex;
        if (content.lastIndexOf(" ", start) != -1) {
            prevIndex = content.lastIndexOf(" ", start);
        } else prevIndex = start;
        if (content.indexOf(" ", end + 30) != -1) {
            lastIndex = content.indexOf(" ", end + 30);
        } else lastIndex = content.indexOf(" ", end);
        String text = content.substring(prevIndex, lastIndex);
        try {
            text = text.replaceAll(word, "<b>" + word + "</b>");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return text;
    }

    private List<StatisticsSearch> getSearchDtoList(List<LemmaEntity> lemmaList, List<String> textLemmaList,
                                                    int offset, int limit) {
        pageRepository.flush();
        if (lemmaList.size() >= textLemmaList.size()) {
            indexRepository.flush();
            List<IndexEntity> foundIndexForPage = findIndexList(lemmaList);
            List<PageEntity> foundPageList = findPageList(foundIndexForPage);
            indexRepository.flush();
            List<IndexEntity> foundIndexList = indexRepository.findByPagesAndLemmas(lemmaList, foundPageList);
            Hashtable<PageEntity, Float> sortedPageByAbsRelevance = getPageAbsRelevance(foundPageList, foundIndexList);
            List<StatisticsSearch> dataList = getSearchData(sortedPageByAbsRelevance, textLemmaList);

            return resultForSearchDtoList(offset, limit, dataList);
        }
        return new ArrayList<>();
    }

    private List<PageEntity> findPageList(List<IndexEntity> foundIndexList) {
        List<PageEntity> foundPageList = new ArrayList<>();
        pageRepository.flush();
        for (IndexEntity index : foundIndexList) {
            foundPageList.add(index.getPage());
        }
        return foundPageList;
    }

    private List<IndexEntity> findIndexList(List<LemmaEntity> lemmaList) {
        List<IndexEntity> foundIndexList = new ArrayList<>();
        indexRepository.flush();
        for (LemmaEntity lemma : lemmaList) {
            foundIndexList.addAll(indexRepository.findByLemmaId(lemma.getId()));
        }
        return foundIndexList;
    }

    private List<StatisticsSearch> resultForSearchDtoList(int offset, int limit, List<StatisticsSearch> dataList) {
        List<StatisticsSearch> result = new ArrayList<>();

        if (offset > dataList.size()) {
            return new ArrayList<>();
        }
        if (dataList.size() > limit) {
            for (int i = offset; i < limit; i++) {
                if (dataList.get(i) != null) {
                    result.add(dataList.get(i));
                }
            }
            return result;
        } else {
            for (StatisticsSearch dto : dataList) {
                if (dto != null) {
                    result.add(dto);
                }
            }
            return result;
        }
    }
    
    

    private Hashtable<PageEntity, Float> getPageAbsRelevance(List<PageEntity> pageList, List<IndexEntity> indexList) {
        Map<PageEntity, Float> pageWithRelevance = pageList.stream()
                .collect(Collectors.toMap(page -> page, page -> indexList.stream()
                        .filter(index -> index.getPage().equals(page))
                        .map(IndexEntity::getRank)
                        .reduce(0f, Float::sum)));

        Map<PageEntity, Float> pageWithAbsRelevance = pageWithRelevance.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue() / Collections.max(pageWithRelevance.values())));

        return pageWithAbsRelevance.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, Hashtable::new));
    }

    @Override
    public ResponseEntity<Object> search(String searchText, String url, int offset, int limit) {
        SearchResponse searchResponse;
        if (searchText.isEmpty()) {
            return new ResponseEntity<>(new FalseResponse(false, "Задан пустой поисковой запрос"), HttpStatus.BAD_REQUEST);
        } else {
            if (!url.isEmpty()) {
                if (siteRepository.findByUrl(url) == null) {
                    return new ResponseEntity<>(new FalseResponse(false, "Страница не найдена"), HttpStatus.BAD_REQUEST);
                } else {
                    searchResponse = siteSearch(searchText, url, offset, limit);
                }
            } else {
                searchResponse = allSiteSearch(searchText, offset, limit);
            }
            return new ResponseEntity<>(searchResponse, HttpStatus.OK);
        }
    }
}
