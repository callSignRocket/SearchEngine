package searchengine.parsers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import searchengine.dto.statistics.StatisticsIndex;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.morphology.Morphology;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.utils.CleanHtmlCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class IndexParserImpl implements IndexParser{

    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final Morphology morphology;
    private List<StatisticsIndex> statisticsIndexList;

    @Override
    public void run(SiteEntity site) {
        Iterable<PageEntity> pageList = pageRepository.findBySite(site);
        List<LemmaEntity> lemmaList = lemmaRepository.findBySite(site);
        statisticsIndexList = new ArrayList<>();

        for (PageEntity page : pageList) {
            if (page.getCode() < 400) {
                long pageId = page.getId();
                String content = page.getContent();
                String title = CleanHtmlCode.clear(content, "title");
                String body = CleanHtmlCode.clear(content, "body");
                HashMap<String, Integer> titleList = morphology.getLemmaList(title);
                HashMap<String, Integer> bodyList = morphology.getLemmaList(body);

                for (LemmaEntity lemma : lemmaList) {
                    Long lemmaId = lemma.getId();
                    String exactLemma = lemma.getLemma();
                    if (titleList.containsKey(exactLemma) || bodyList.containsKey(exactLemma)) {
                        float totalRank = 0.0F;
                        if (titleList.get(exactLemma) != null) {
                            Float titleRank = Float.valueOf(titleList.get(exactLemma));
                            totalRank += titleRank;
                        }
                        if (bodyList.get(exactLemma) != null) {
                            float bodyRank = (float) (bodyList.get(exactLemma) * 0.8);
                            totalRank += bodyRank;
                        }
                        statisticsIndexList.add(new StatisticsIndex(pageId, lemmaId, totalRank));
                    } else {
                        log.debug("Лемма не найдена");
                    }
                }
            } else {
                log.debug("Bad status code: " + page.getCode());
            }
        }
    }

    @Override
    public List<StatisticsIndex> getIndexList() {
        return statisticsIndexList;
    }
}
