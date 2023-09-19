package searchengine.parsers;

import searchengine.dto.statistics.StatisticsLemma;
import searchengine.model.SiteEntity;

import java.util.List;

public interface LemmaParser {
    void run(SiteEntity site);
    List<StatisticsLemma> getLemmaDtoList();
}
