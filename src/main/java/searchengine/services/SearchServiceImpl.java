package searchengine.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
    public List<StatisticsSearch> allSiteSearch(String searchText, int offset, int limit) {
        log.info("Результат поиска: " + searchText);
        List<SiteEntity> siteList = siteRepository.findAll();
        List<StatisticsSearch> result = new ArrayList<>();
        List<LemmaEntity> foundLemmaList = new ArrayList<>();
        List<String> textLemmaList = getLemmaFromSearchText(searchText);
        for (SiteEntity site : siteList) {
            foundLemmaList.addAll(getLemmaListFromSite(textLemmaList, site));
        }
        List<StatisticsSearch> searchData = null;
        for (LemmaEntity lemma : foundLemmaList) {
            if (lemma.getLemma().equals(searchText)) {
                searchData = new ArrayList<>(getSearchDtoList(foundLemmaList, textLemmaList, offset, limit));
                searchData.sort((o1, o2) -> Float.compare(o2.relevance(), o1.relevance()));
                if (searchData.size() > limit) {
                    for (int i = offset; i < limit; i++) {
                        result.add(searchData.get(i));
                    }
                    return result;
                }
            } else throw new RuntimeException();
        }
        log.info("Поиск завершен.");
        return searchData;
    }

    @Override
    public List<StatisticsSearch> siteSearch(String searchText, String url, int offset, int limit) {
        log.info("Поиск: " + "«" + searchText + "»" + " на странице: " + url);
        SiteEntity site = siteRepository.findByUrl(url);
        List<String> textLemmaList = getLemmaFromSearchText(searchText);
        List<LemmaEntity> foundLemmaList = getLemmaListFromSite(textLemmaList, site);
        log.info("Поиск завершен.");
        return getSearchDtoList(foundLemmaList, textLemmaList, offset, limit);
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
        List<LemmaEntity> lemmaList = lemmaRepository.findLemmaListBySite(lemmas, site);
        List<LemmaEntity> result = new ArrayList<>(lemmaList);
        result.sort(Comparator.comparingInt(LemmaEntity::getFrequency));
        return result;
    }

    private List<StatisticsSearch> getSearchData(Hashtable<PageEntity, Float> pageList, List<String> textLemmaList) {
        return pageList.keySet().stream()
                .map(page -> {
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
                    return new StatisticsSearch(site, siteName, uri, title, snippet , absRelevance);
        }).toList();
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
        return result.stream().sorted(Comparator.comparingInt(String::length).reversed()).toList();
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
        List<StatisticsSearch> result = new ArrayList<>();
        pageRepository.flush();
        if (lemmaList.size() >= textLemmaList.size()) {
            Set<PageEntity> foundPageSet = new HashSet<>(pageRepository.findByLemmaList(lemmaList));
            indexRepository.flush();
            List<PageEntity> foundPageList = new ArrayList<>(foundPageSet);
            List<IndexEntity> foundIndexList = indexRepository.findByPagesAndLemmas(lemmaList, foundPageList);
            Hashtable<PageEntity, Float> sortedPageByAbsRelevance = getPageAbsRelevance(foundPageList, foundIndexList);
            List<StatisticsSearch> dataList = getSearchData(sortedPageByAbsRelevance, textLemmaList);

            if (offset > dataList.size()) {
                return new ArrayList<>();
            }
            if (dataList.size() > limit) {
                for (int i = offset; i < limit; i++) {
                    result.add(dataList.get(i));
                }
                return result;
            } else return dataList;
        }else return result;
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
}
