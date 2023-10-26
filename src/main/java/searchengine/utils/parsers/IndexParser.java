package searchengine.utils.parsers;

import searchengine.dto.statistics.StatisticsIndex;
import searchengine.model.SiteEntity;

import java.util.List;

public interface IndexParser {
    void run(SiteEntity site);
    List<StatisticsIndex> getIndexList();
}
