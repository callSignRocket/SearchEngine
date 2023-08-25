package searchengine.dto.statistics;

import java.util.List;

public record SearchResponse(boolean result,
                             int count,
                             List<StatisticsSearch> data) {
}
