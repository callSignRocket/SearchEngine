package searchengine.utils.parsers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.dto.statistics.StatisticsLemma;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.utils.morphology.Morphology;
import searchengine.repositories.PageRepository;
import searchengine.utils.CleanHtmlCode;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@RequiredArgsConstructor
@Slf4j
public class LemmaParserImpl implements LemmaParser {
    private final PageRepository pageRepository;
    private final Morphology morphology;
    private List<StatisticsLemma> statisticsLemmaList;

    @Override
    public void run(SiteEntity site) {
        statisticsLemmaList = new CopyOnWriteArrayList<>();
        Iterable<PageEntity> pageList = pageRepository.findBySite(site);
        TreeMap<String, Integer> lemmaList = new TreeMap<>();
        for (PageEntity page : pageList) {
            String content = page.getContent();
            String title = CleanHtmlCode.clear(content, "title");
            String body = CleanHtmlCode.clear(content, "body");
            HashMap<String, Integer> titleList = morphology.getLemmaList(title);
            HashMap<String, Integer> bodyList = morphology.getLemmaList(body);
            Set<String> allWords = new HashSet<>();
            allWords.addAll(titleList.keySet());
            allWords.addAll(bodyList.keySet());
            for (String word : allWords) {
                int frequency = lemmaList.getOrDefault(word, 0) + 1;
                lemmaList.put(word, frequency);
            }
        }
        for (String lemma : lemmaList.keySet()) {
            Integer frequency = lemmaList.get(lemma);
            statisticsLemmaList.add(new StatisticsLemma(lemma, frequency));
        }
    }

    @Override
    public List<StatisticsLemma> getLemmaDtoList() {
        return statisticsLemmaList;
    }
}
